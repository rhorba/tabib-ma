import { test, expect } from '@playwright/test'

// Epic 2 smoke suite (Story 2.1 + 2.2) — runs against the real backend, not MSW mocks.
// Exercises the seeded PLATFORM_ADMIN from V3__seed_platform_admin.sql.
function uniqueEmail(prefix: string) {
  return `${prefix}.${Date.now()}.${Math.floor(Math.random() * 1000)}@example.com`
}

test('doctor creates a profile, uploads a document, and platform admin approves it', async ({
  page,
}) => {
  const email = uniqueEmail('doctor')

  await page.goto('/register')
  await page.getByLabel('Prénom').fill('Amina')
  await page.getByLabel('Nom', { exact: true }).fill('Bennani')
  await page.getByLabel('Adresse e-mail').fill(email)
  await page.getByLabel('Mot de passe').fill('Sup3rSecret!')
  await page.getByRole('combobox').click()
  await page.getByRole('option', { name: 'Médecin' }).click()
  await page.getByRole('button', { name: 'Créer mon compte' }).click()
  await expect(page).toHaveURL('/')

  await page.getByRole('link', { name: 'Mon profil médecin' }).click()
  await page.getByLabel('Spécialité').fill('Cardiologie')
  await page.getByLabel('Ville').fill('Rabat')
  await page.getByLabel('Tarif de consultation (MAD)').fill('300')
  await page.getByRole('button', { name: 'Créer mon profil' }).click()
  await expect(page.getByText('En attente de vérification')).toBeVisible()

  await page.getByLabel('Fichier').setInputFiles({
    name: 'license.pdf',
    mimeType: 'application/pdf',
    buffer: Buffer.from('%PDF-1.4 minimal test fixture'),
  })
  await page.getByRole('button', { name: 'Téléverser' }).click()
  await expect(page.getByRole('listitem')).toContainText('Licence médicale')

  await page.getByRole('button', { name: 'Déconnexion' }).click()
  await page.goto('/login')
  await page.getByLabel('Adresse e-mail').fill('tabib-admin@tabibma.dev')
  await page.getByLabel('Mot de passe').fill('changeme-admin-dev-only')
  await page.getByRole('button', { name: 'Se connecter' }).click()
  await expect(page).toHaveURL('/')

  await page.getByRole('link', { name: 'File de vérification' }).click()
  const queueItem = page.getByRole('listitem').filter({ hasText: 'Cardiologie' })
  await expect(queueItem).toBeVisible()
  await queueItem.getByRole('button', { name: 'Voir les documents' }).click()
  await expect(queueItem.getByText('Licence médicale')).toBeVisible()
  await queueItem.getByRole('button', { name: 'Approuver' }).click()
  await expect(queueItem).not.toBeVisible()

  await page.getByRole('button', { name: 'Déconnexion' }).click()
  await page.goto('/login')
  await page.getByLabel('Adresse e-mail').fill(email)
  await page.getByLabel('Mot de passe').fill('Sup3rSecret!')
  await page.getByRole('button', { name: 'Se connecter' }).click()
  await page.getByRole('link', { name: 'Mon profil médecin' }).click()
  await expect(page.getByText('Vérifié')).toBeVisible()
})

test('a patient cannot reach doctor onboarding or the verification queue', async ({ page }) => {
  const email = uniqueEmail('patient')

  await page.goto('/register')
  await page.getByLabel('Prénom').fill('Youssef')
  await page.getByLabel('Nom', { exact: true }).fill('Amrani')
  await page.getByLabel('Adresse e-mail').fill(email)
  await page.getByLabel('Mot de passe').fill('Sup3rSecret!')
  await page.getByRole('button', { name: 'Créer mon compte' }).click()
  await expect(page).toHaveURL('/')

  await page.goto('/doctor/onboarding')
  await expect(page).toHaveURL('/')

  await page.goto('/platform-admin/verification-queue')
  await expect(page).toHaveURL('/')
})
