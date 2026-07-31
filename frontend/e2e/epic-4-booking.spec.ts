import { test, expect, type Page } from '@playwright/test'

// Epic 4 smoke suite (Stories 4.1-4.4 + 5.1) — runs against the real backend, not MSW
// mocks. Exercises the seeded PLATFORM_ADMIN from V3__seed_platform_admin.sql. Specialties
// are unique per run so result-count assertions aren't affected by prior runs' accumulated
// data in the persistent dev DB (same convention as epic-3-search.spec.ts).
const DOCTOR_PASSWORD = 'Sup3rSecret!'

function unique(prefix: string) {
  return `${prefix}-${Date.now()}-${Math.floor(Math.random() * 1000)}`
}

function uniqueEmail(prefix: string) {
  return `${prefix}.${Date.now()}.${Math.floor(Math.random() * 1000)}@example.com`
}

/** Registers a doctor, creates their profile, and sets one recurring Monday
 * 09:00-17:00 rule (the form's own defaults) before generating slots — all
 * while still logged in, since availability doesn't require verification. */
async function setUpDoctorWithSlots(page: Page, specialty: string) {
  const email = uniqueEmail('doctor')
  await page.goto('/register')
  await page.getByLabel('Prénom').fill('Amina')
  await page.getByLabel('Nom', { exact: true }).fill('Doctor')
  await page.getByLabel('Adresse e-mail').fill(email)
  await page.getByLabel('Mot de passe').fill(DOCTOR_PASSWORD)
  await page.getByRole('combobox').click()
  await page.getByRole('option', { name: 'Médecin' }).click()
  await page.getByRole('button', { name: 'Créer mon compte' }).click()
  await expect(page).toHaveURL('/')

  await page.getByRole('link', { name: 'Mon profil médecin' }).click()
  await page.getByLabel('Spécialité').fill(specialty)
  await page.getByLabel('Ville').fill('Rabat')
  await page.getByLabel('Tarif de consultation (MAD)').fill('250')
  await page.getByRole('button', { name: 'Créer mon profil' }).click()
  await expect(page.getByText('En attente de vérification')).toBeVisible()

  await page.getByRole('link', { name: 'Mes disponibilités' }).click()
  await page.getByRole('button', { name: 'Ajouter ce créneau récurrent' }).click()
  await expect(page.getByText(/Lundi · 09:00–17:00/)).toBeVisible()
  await page.getByRole('button', { name: 'Générer les créneaux des 30 prochains jours' }).click()
  await expect(page.getByText(/créneaux générés/)).toBeVisible()

  await page.getByRole('button', { name: 'Déconnexion' }).click()
  return email
}

async function approveDoctor(page: Page, specialty: string) {
  await page.goto('/login')
  await page.getByLabel('Adresse e-mail').fill('tabib-admin@tabibma.dev')
  await page.getByLabel('Mot de passe').fill('changeme-admin-dev-only')
  await page.getByRole('button', { name: 'Se connecter' }).click()
  await page.getByRole('link', { name: 'File de vérification' }).click()
  const queueItem = page.getByRole('listitem').filter({ hasText: specialty })
  await queueItem.getByRole('button', { name: 'Approuver' }).click()
  await expect(queueItem).not.toBeVisible()
  await page.getByRole('button', { name: 'Déconnexion' }).click()
}

async function registerPatient(page: Page, firstName: string) {
  const email = uniqueEmail('patient')
  await page.goto('/register')
  await page.getByLabel('Prénom').fill(firstName)
  await page.getByLabel('Nom', { exact: true }).fill('Patient')
  await page.getByLabel('Adresse e-mail').fill(email)
  await page.getByLabel('Mot de passe').fill(DOCTOR_PASSWORD)
  await page.getByRole('button', { name: 'Créer mon compte' }).click()
  await expect(page).toHaveURL('/')
  return email
}

async function searchAndOpenProfile(page: Page, specialty: string) {
  await page.getByRole('link', { name: 'Rechercher un médecin' }).click()
  await page.getByLabel('Spécialité').fill(specialty)
  await page.getByRole('button', { name: 'Rechercher' }).click()
  await page.getByRole('link', { name: 'Voir le profil' }).click()
}

test('a patient books and pays for an appointment, cancels it, and the slot reopens', async ({
  page,
}) => {
  const specialty = unique('Cardiologie')
  await setUpDoctorWithSlots(page, specialty)
  await approveDoctor(page, specialty)

  await registerPatient(page, 'Youssef')
  await searchAndOpenProfile(page, specialty)
  await page.getByRole('link', { name: 'Prendre rendez-vous' }).click()
  const slotButtons = page.getByRole('button', { name: /^\d{1,2}\/\d{1,2}\/\d{4}/ })
  await expect(slotButtons.first()).toBeVisible()
  const bookedSlotLabel = await slotButtons.first().textContent()
  await slotButtons.first().click()
  await page.getByRole('button', { name: 'Confirmer et payer' }).click()
  await expect(page.getByText('Votre rendez-vous est confirmé.')).toBeVisible()

  await page.getByRole('link', { name: 'Voir mes rendez-vous' }).click()
  await expect(page.getByText('Confirmé')).toBeVisible()
  await page.getByRole('button', { name: 'Annuler' }).click()
  await expect(page.getByText('Annulé')).toBeVisible()
  await expect(page.getByRole('button', { name: 'Annuler' })).not.toBeVisible()
  await expect(page.getByRole('button', { name: 'Reprogrammer' })).not.toBeVisible()

  // The cancelled slot should be bookable again — proves CancellationService
  // actually released it, not just marked the appointment CANCELLED.
  await searchAndOpenProfile(page, specialty)
  await page.getByRole('link', { name: 'Prendre rendez-vous' }).click()
  await expect(page.getByRole('button', { name: bookedSlotLabel! })).toBeVisible()
})

test('two patients racing for the same slot — exactly one is confirmed', async ({ browser }, testInfo) => {
  // `video: 'on'` in playwright.config.ts only auto-applies to the default
  // `page` fixture — contexts created manually (needed here for two truly
  // independent patient sessions) need recordVideo passed explicitly to be
  // picked up by scripts/collect-e2e-video.mjs's directory scan.
  const recordVideo = { dir: testInfo.outputPath('video') }
  const specialty = unique('Dermatologie')
  const setupPage = await browser.newPage({ recordVideo })
  await setUpDoctorWithSlots(setupPage, specialty)
  await approveDoctor(setupPage, specialty)
  await setupPage.close()

  const contextA = await browser.newContext({ recordVideo })
  const contextB = await browser.newContext({ recordVideo })
  const pageA = await contextA.newPage()
  const pageB = await contextB.newPage()

  await registerPatient(pageA, 'PatientA')
  await registerPatient(pageB, 'PatientB')
  await searchAndOpenProfile(pageA, specialty)
  await searchAndOpenProfile(pageB, specialty)
  await pageA.getByRole('link', { name: 'Prendre rendez-vous' }).click()
  await pageB.getByRole('link', { name: 'Prendre rendez-vous' }).click()

  const slotButtonA = pageA.getByRole('button', { name: /^\d{1,2}\/\d{1,2}\/\d{4}/ }).first()
  const slotButtonB = pageB.getByRole('button', { name: /^\d{1,2}\/\d{1,2}\/\d{4}/ }).first()
  await expect(slotButtonA).toBeVisible()
  await expect(slotButtonB).toBeVisible()
  await slotButtonA.click()
  await slotButtonB.click()

  const confirmA = pageA.getByRole('button', { name: 'Confirmer et payer' })
  const confirmB = pageB.getByRole('button', { name: 'Confirmer et payer' })
  await Promise.all([confirmA.click(), confirmB.click()])

  const outcomeA = pageA.getByText('Votre rendez-vous est confirmé.')
  const outcomeB = pageB.getByText('Votre rendez-vous est confirmé.')
  const conflictA = pageA.getByText("vient d'être réservé")
  const conflictB = pageB.getByText("vient d'être réservé")

  await Promise.race([outcomeA.waitFor(), conflictA.waitFor()])
  await Promise.race([outcomeB.waitFor(), conflictB.waitFor()])

  const [aConfirmed, bConfirmed] = await Promise.all([
    outcomeA.isVisible(),
    outcomeB.isVisible(),
  ])
  // Exactly one of the two racing patients won the slot.
  expect(aConfirmed !== bConfirmed).toBe(true)

  await contextA.close()
  await contextB.close()
})
