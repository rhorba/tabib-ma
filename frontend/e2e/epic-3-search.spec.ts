import { test, expect } from '@playwright/test'

// Epic 3 smoke suite (Story 3.1 search + Story 3.2 public profile) — runs against the
// real backend, not MSW mocks. Exercises the seeded PLATFORM_ADMIN from
// V3__seed_platform_admin.sql. Specialties are unique per run so result-count assertions
// aren't affected by prior runs' accumulated data in the persistent dev DB.
function unique(prefix: string) {
  return `${prefix}-${Date.now()}-${Math.floor(Math.random() * 1000)}`
}

function uniqueEmail(prefix: string) {
  return `${prefix}.${Date.now()}.${Math.floor(Math.random() * 1000)}@example.com`
}

test('a patient searches for an approved doctor and views their public profile', async ({
  page,
}) => {
  const doctorEmail = uniqueEmail('doctor')
  const specialty = unique('Endocrinologie')

  await page.goto('/register')
  await page.getByLabel('Prénom').fill('Nadia')
  await page.getByLabel('Nom', { exact: true }).fill('Chraibi')
  await page.getByLabel('Adresse e-mail').fill(doctorEmail)
  await page.getByLabel('Mot de passe').fill('Sup3rSecret!')
  await page.getByRole('combobox').click()
  await page.getByRole('option', { name: 'Médecin' }).click()
  await page.getByRole('button', { name: 'Créer mon compte' }).click()
  await expect(page).toHaveURL('/')

  await page.getByRole('link', { name: 'Mon profil médecin' }).click()
  await page.getByLabel('Spécialité').fill(specialty)
  await page.getByLabel('Ville').fill('Tanger')
  await page.getByLabel('Tarif de consultation (MAD)').fill('280')
  await page.getByRole('button', { name: 'Créer mon profil' }).click()
  await expect(page.getByText('En attente de vérification')).toBeVisible()
  await page.getByRole('button', { name: 'Déconnexion' }).click()

  await page.goto('/login')
  await page.getByLabel('Adresse e-mail').fill('tabib-admin@tabibma.dev')
  await page.getByLabel('Mot de passe').fill('changeme-admin-dev-only')
  await page.getByRole('button', { name: 'Se connecter' }).click()
  await page.getByRole('link', { name: 'File de vérification' }).click()
  const queueItem = page.getByRole('listitem').filter({ hasText: specialty })
  await queueItem.getByRole('button', { name: 'Approuver' }).click()
  await expect(queueItem).not.toBeVisible()
  await page.getByRole('button', { name: 'Déconnexion' }).click()

  const patientEmail = uniqueEmail('patient')
  await page.goto('/register')
  await page.getByLabel('Prénom').fill('Younes')
  await page.getByLabel('Nom', { exact: true }).fill('Berrada')
  await page.getByLabel('Adresse e-mail').fill(patientEmail)
  await page.getByLabel('Mot de passe').fill('Sup3rSecret!')
  await page.getByRole('button', { name: 'Créer mon compte' }).click()
  await expect(page).toHaveURL('/')

  await page.getByRole('link', { name: 'Rechercher un médecin' }).click()
  await page.getByLabel('Spécialité').fill(specialty)
  await page.getByLabel('Ville').fill('Tanger')
  await page.getByRole('button', { name: 'Rechercher' }).click()
  await expect(page.getByText('Nadia Chraibi')).toBeVisible()

  await page.getByRole('link', { name: 'Voir le profil' }).click()
  await expect(page.getByRole('heading', { name: 'Nadia Chraibi' })).toBeVisible()
  await expect(page.getByText(specialty)).toBeVisible()
  await expect(page.getByText("Pas encore d'avis")).toBeVisible()
})

test('search excludes an unapproved doctor, and a nonexistent profile shows not-found', async ({
  page,
}) => {
  const doctorEmail = uniqueEmail('doctor-pending')
  const specialty = unique('Rhumatologie')

  await page.goto('/register')
  await page.getByLabel('Prénom').fill('Samir')
  await page.getByLabel('Nom', { exact: true }).fill('Ouazzani')
  await page.getByLabel('Adresse e-mail').fill(doctorEmail)
  await page.getByLabel('Mot de passe').fill('Sup3rSecret!')
  await page.getByRole('combobox').click()
  await page.getByRole('option', { name: 'Médecin' }).click()
  await page.getByRole('button', { name: 'Créer mon compte' }).click()
  await expect(page).toHaveURL('/')

  await page.getByRole('link', { name: 'Mon profil médecin' }).click()
  await page.getByLabel('Spécialité').fill(specialty)
  await page.getByLabel('Ville').fill('Kenitra')
  await page.getByLabel('Tarif de consultation (MAD)').fill('220')
  await page.getByRole('button', { name: 'Créer mon profil' }).click()
  await expect(page.getByText('En attente de vérification')).toBeVisible()

  await page.getByRole('link', { name: 'Rechercher un médecin' }).click()
  await page.getByLabel('Spécialité').fill(specialty)
  await page.getByRole('button', { name: 'Rechercher' }).click()
  await expect(page.getByText('Aucun médecin trouvé.')).toBeVisible()

  await page.goto('/doctors/00000000-0000-0000-0000-000000000000')
  await expect(page.getByText("Ce médecin n'est pas disponible.")).toBeVisible()
})
