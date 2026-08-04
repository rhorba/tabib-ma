import { test, expect, type Page } from '@playwright/test'

// Epic 8 smoke suite (Story 8.1, clinic booking volume + revenue dashboard) —
// runs against the real backend, not MSW mocks. Exercises the seeded
// CLINIC_ADMIN from V4__clinic_invitations.sql, which is reused/accumulates
// data across runs in the persistent dev DB — so this asserts the *delta*
// caused by this test's own booking rather than an absolute count, the same
// fix applied to the Epic 3 verification-queue e2e assertions.
const PASSWORD = 'Sup3rSecret!'
const CLINIC_ADMIN_EMAIL = 'clinic-admin@tabibma.dev'
const CLINIC_ADMIN_PASSWORD = 'changeme-clinicadmin-dev-only'
const PLATFORM_ADMIN_EMAIL = 'tabib-admin@tabibma.dev'
const PLATFORM_ADMIN_PASSWORD = 'changeme-admin-dev-only'
const CONSULTATION_FEE_MAD = 250

function unique(prefix: string) {
  return `${prefix}-${Date.now()}-${Math.floor(Math.random() * 1000)}`
}

function uniqueEmail(prefix: string) {
  return `${prefix}.${Date.now()}.${Math.floor(Math.random() * 1000)}@example.com`
}

async function setUpDoctorWithSlots(page: Page, specialty: string) {
  const email = uniqueEmail('doctor')
  await page.goto('/register')
  await page.getByLabel('Prénom').fill('Amina')
  await page.getByLabel('Nom', { exact: true }).fill('Doctor')
  await page.getByLabel('Adresse e-mail').fill(email)
  await page.getByLabel('Mot de passe').fill(PASSWORD)
  await page.getByRole('combobox').click()
  await page.getByRole('option', { name: 'Médecin' }).click()
  await page.getByRole('button', { name: 'Créer mon compte' }).click()
  await expect(page).toHaveURL('/')

  await page.getByRole('link', { name: 'Mon profil médecin' }).click()
  await page.getByLabel('Spécialité').fill(specialty)
  await page.getByLabel('Ville').fill('Rabat')
  await page.getByLabel('Tarif de consultation (MAD)').fill(String(CONSULTATION_FEE_MAD))
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
  await page.getByLabel('Adresse e-mail').fill(PLATFORM_ADMIN_EMAIL)
  await page.getByLabel('Mot de passe').fill(PLATFORM_ADMIN_PASSWORD)
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
  await page.getByLabel('Mot de passe').fill(PASSWORD)
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

async function loginAsClinicAdmin(page: Page) {
  await page.goto('/login')
  await page.getByLabel('Adresse e-mail').fill(CLINIC_ADMIN_EMAIL)
  await page.getByLabel('Mot de passe').fill(CLINIC_ADMIN_PASSWORD)
  await page.getByRole('button', { name: 'Se connecter' }).click()
  await expect(page).toHaveURL('/')
}

/** Reads the dashboard card's two metrics as numbers. Assumes the caller is
 * already on /clinic-admin with a clinic (dashboard card only renders once
 * `clinicQuery.data` exists). */
async function readDashboardMetrics(page: Page) {
  await expect(page.getByRole('heading', { name: 'Tableau de bord' })).toBeVisible()
  const bookingVolumeText = await page
    .locator('dt:has-text("Rendez-vous réservés") + dd')
    .textContent()
  const revenueText = await page.locator('dt:has-text("Revenu (MAD)") + dd').textContent()
  return {
    bookingVolume: Number(bookingVolumeText),
    revenueMad: Number(revenueText),
  }
}

test('a clinic admin sees a real booking reflected in the dashboard', async ({ page }) => {
  const specialty = unique('Gastro-entérologie')
  const doctorEmail = await setUpDoctorWithSlots(page, specialty)
  await approveDoctor(page, specialty)

  // Clinic admin creates a clinic if it doesn't already have one (1:1,
  // reused dev/test bootstrap across runs), then invites the doctor.
  await loginAsClinicAdmin(page)
  await page.getByRole('link', { name: 'Ma clinique' }).click()
  const clinicNameInput = page.getByLabel('Nom de la clinique')
  if (await clinicNameInput.isVisible().catch(() => false)) {
    await clinicNameInput.fill('Clinique Al Amal E2E')
    await page.getByLabel('Ville').fill('Rabat')
    await page.getByRole('button', { name: 'Créer ma clinique' }).click()
  }
  await expect(page.getByRole('button', { name: 'Inviter' })).toBeVisible()
  await page.getByLabel('Adresse e-mail du médecin').fill(doctorEmail)
  await page.getByRole('button', { name: 'Inviter' }).click()
  const invitationRow = page.getByRole('listitem').filter({ hasText: doctorEmail })
  await expect(invitationRow).toContainText('En attente')

  const before = await readDashboardMetrics(page)
  await page.getByRole('button', { name: 'Déconnexion' }).click()

  // Doctor accepts the invitation, joining the clinic's staff.
  await page.goto('/login')
  await page.getByLabel('Adresse e-mail').fill(doctorEmail)
  await page.getByLabel('Mot de passe').fill(PASSWORD)
  await page.getByRole('button', { name: 'Se connecter' }).click()
  await page.getByRole('link', { name: 'Mon profil médecin' }).click()
  await page.getByRole('button', { name: 'Accepter' }).click()
  await expect(page.getByText('Invitations de clinique en attente')).not.toBeVisible()
  await page.getByRole('button', { name: 'Déconnexion' }).click()

  // A patient books and pays for a real appointment with the now-affiliated doctor.
  await registerPatient(page, 'Nadia')
  await searchAndOpenProfile(page, specialty)
  await page.getByRole('link', { name: 'Prendre rendez-vous' }).click()
  await page.getByRole('button', { name: /^\d{1,2}\/\d{1,2}\/\d{4}/ }).first().click()
  await page.getByRole('button', { name: 'Confirmer et payer' }).click()
  await expect(page.getByText('Votre rendez-vous est confirmé.')).toBeVisible()
  await page.getByRole('button', { name: 'Déconnexion' }).click()

  // Dashboard now reflects the new booking and its revenue.
  await loginAsClinicAdmin(page)
  await page.getByRole('link', { name: 'Ma clinique' }).click()
  const after = await readDashboardMetrics(page)
  expect(after.bookingVolume - before.bookingVolume).toBe(1)
  expect(after.revenueMad - before.revenueMad).toBe(CONSULTATION_FEE_MAD)
})
