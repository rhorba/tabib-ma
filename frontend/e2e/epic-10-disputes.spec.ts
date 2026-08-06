import { test, expect, type Page } from '@playwright/test'

// Epic 10 smoke suite (Story 10.1 dispute queue + Story 10.2 admin actions) — runs
// against the real backend, not MSW mocks. Covers the full loop: a patient
// self-reports a problem on a real paid appointment, the seeded PLATFORM_ADMIN sees
// it enriched in the queue, refunds the payment, force-cancels the appointment, then
// resolves the dispute. The seeded platform admin account accumulates open disputes
// across runs in the persistent dev DB, so this test always locates its own dispute
// by a unique reason string rather than assuming it's the only item in the queue —
// same fix pattern as every prior epic's dashboard/queue e2e assertions.
const PASSWORD = 'Sup3rSecret!'
const PLATFORM_ADMIN_EMAIL = 'tabib-admin@tabibma.dev'
const PLATFORM_ADMIN_PASSWORD = 'changeme-admin-dev-only'

function unique(prefix: string) {
  return `${prefix}-${Date.now()}-${Math.floor(Math.random() * 1000)}`
}

function uniqueEmail(prefix: string) {
  return `${prefix}.${Date.now()}.${Math.floor(Math.random() * 1000)}@example.com`
}

async function setUpDoctorWithSlots(page: Page, specialty: string) {
  const email = uniqueEmail('doctor')
  await page.goto('/register')
  await page.getByLabel('Prénom').fill('Sara')
  await page.getByLabel('Nom', { exact: true }).fill('Doctor')
  await page.getByLabel('Adresse e-mail').fill(email)
  await page.getByLabel('Mot de passe').fill(PASSWORD)
  await page.getByRole('combobox').click()
  await page.getByRole('option', { name: 'Médecin' }).click()
  await page.getByRole('button', { name: 'Créer mon compte' }).click()
  await expect(page).toHaveURL('/')

  await page.getByRole('link', { name: 'Mon profil médecin' }).click()
  await page.getByLabel('Spécialité').fill(specialty)
  await page.getByLabel('Ville').fill('Fès')
  await page.getByLabel('Tarif de consultation (MAD)').fill('275')
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

async function loginAsPlatformAdmin(page: Page) {
  await page.goto('/login')
  await page.getByLabel('Adresse e-mail').fill(PLATFORM_ADMIN_EMAIL)
  await page.getByLabel('Mot de passe').fill(PLATFORM_ADMIN_PASSWORD)
  await page.getByRole('button', { name: 'Se connecter' }).click()
  await expect(page).toHaveURL('/')
}

async function approveDoctor(page: Page, specialty: string) {
  await loginAsPlatformAdmin(page)
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

test('a patient reports a problem, then a platform admin refunds, force-cancels, and resolves it', async ({
  page,
}) => {
  const specialty = unique('Endocrinologie')
  const reason = unique('Le medecin ne s est jamais presente au rendez-vous')

  await setUpDoctorWithSlots(page, specialty)
  await approveDoctor(page, specialty)

  // A patient books and pays for a real appointment, then reports it.
  await registerPatient(page, 'Nadia')
  await page.getByRole('link', { name: 'Rechercher un médecin' }).click()
  await page.getByLabel('Spécialité').fill(specialty)
  await page.getByRole('button', { name: 'Rechercher' }).click()
  await page.getByRole('link', { name: 'Voir le profil' }).click()
  await page.getByRole('link', { name: 'Prendre rendez-vous' }).click()
  await page.getByRole('button', { name: /^\d{1,2}\/\d{1,2}\/\d{4}/ }).first().click()
  await page.getByRole('button', { name: 'Confirmer et payer' }).click()
  await expect(page.getByText('Votre rendez-vous est confirmé.')).toBeVisible()

  await page.getByRole('link', { name: 'Mes rendez-vous', exact: true }).click()
  await page.getByRole('button', { name: 'Signaler un problème' }).click()
  await page.getByLabel('Description').fill(reason)
  await page.getByRole('button', { name: 'Envoyer le signalement' }).click()
  await expect(page.getByText('Problème signalé')).toBeVisible()
  await page.getByRole('button', { name: 'Déconnexion' }).click()

  // The platform admin sees the enriched dispute in the queue and acts on it.
  await loginAsPlatformAdmin(page)
  await page.getByRole('link', { name: 'Litiges' }).click()
  const disputeItem = page.getByRole('listitem').filter({ hasText: reason })
  await expect(disputeItem).toContainText('Nadia Patient')
  await expect(disputeItem).toContainText('Sara Doctor')
  await expect(disputeItem).toContainText('Réclamation')

  await disputeItem.getByRole('button', { name: 'Rembourser' }).click()
  await expect(disputeItem.getByText('Paiement remboursé.')).toBeVisible()

  await disputeItem.getByRole('button', { name: 'Annuler le rendez-vous' }).click()
  await expect(disputeItem.getByText('Rendez-vous annulé.')).toBeVisible()

  await disputeItem.getByRole('button', { name: 'Marquer comme résolu' }).click()
  await expect(disputeItem).not.toBeVisible()
})
