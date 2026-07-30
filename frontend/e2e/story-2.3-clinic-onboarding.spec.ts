import { test, expect } from '@playwright/test'

// Story 2.3 smoke suite (clinic self-service creation + doctor invitation) —
// runs against the real backend, not MSW mocks. Exercises the seeded
// CLINIC_ADMIN from V4__clinic_invitations.sql.
function uniqueEmail(prefix: string) {
  return `${prefix}.${Date.now()}.${Math.floor(Math.random() * 1000)}@example.com`
}

test('clinic admin creates a clinic, invites a doctor, and the doctor accepts', async ({
  page,
}) => {
  const doctorEmail = uniqueEmail('doctor')

  // Doctor registers and creates a profile first — accepting an invitation
  // requires an existing doctor profile.
  await page.goto('/register')
  await page.getByLabel('Prénom').fill('Salma')
  await page.getByLabel('Nom', { exact: true }).fill('El Idrissi')
  await page.getByLabel('Adresse e-mail').fill(doctorEmail)
  await page.getByLabel('Mot de passe').fill('Sup3rSecret!')
  await page.getByRole('combobox').click()
  await page.getByRole('option', { name: 'Médecin' }).click()
  await page.getByRole('button', { name: 'Créer mon compte' }).click()
  await expect(page).toHaveURL('/')

  await page.getByRole('link', { name: 'Mon profil médecin' }).click()
  await page.getByLabel('Spécialité').fill('Dermatologie')
  await page.getByLabel('Ville').fill('Marrakech')
  await page.getByLabel('Tarif de consultation (MAD)').fill('250')
  await page.getByRole('button', { name: 'Créer mon profil' }).click()
  await expect(page.getByText('En attente de vérification')).toBeVisible()

  await page.getByRole('button', { name: 'Déconnexion' }).click()

  // Seeded clinic admin logs in, creates a clinic if it doesn't already have
  // one (the admin_user_id -> clinic relationship is 1:1 and this account is
  // reused dev/test bootstrap, so a prior run may already have created it),
  // and invites the doctor.
  await page.goto('/login')
  await page.getByLabel('Adresse e-mail').fill('clinic-admin@tabibma.dev')
  await page.getByLabel('Mot de passe').fill('changeme-clinicadmin-dev-only')
  await page.getByRole('button', { name: 'Se connecter' }).click()
  await expect(page).toHaveURL('/')

  await page.getByRole('link', { name: 'Ma clinique' }).click()
  const clinicNameInput = page.getByLabel('Nom de la clinique')
  if (await clinicNameInput.isVisible().catch(() => false)) {
    await clinicNameInput.fill('Clinique Atlas')
    await page.getByLabel('Ville').fill('Casablanca')
    await page.getByRole('button', { name: 'Créer ma clinique' }).click()
  }
  await expect(page.getByRole('button', { name: 'Inviter' })).toBeVisible()

  await page.getByLabel('Adresse e-mail du médecin').fill(doctorEmail)
  await page.getByRole('button', { name: 'Inviter' }).click()
  const invitationRow = page.getByRole('listitem').filter({ hasText: doctorEmail })
  await expect(invitationRow).toContainText('En attente')

  await page.getByRole('button', { name: 'Déconnexion' }).click()

  // Doctor sees and accepts the invitation.
  await page.goto('/login')
  await page.getByLabel('Adresse e-mail').fill(doctorEmail)
  await page.getByLabel('Mot de passe').fill('Sup3rSecret!')
  await page.getByRole('button', { name: 'Se connecter' }).click()
  await page.getByRole('link', { name: 'Mon profil médecin' }).click()
  await expect(page.getByText('Invitations de clinique en attente')).toBeVisible()
  await page.getByRole('button', { name: 'Accepter' }).click()
  await expect(page.getByText('Invitations de clinique en attente')).not.toBeVisible()

  await page.getByRole('button', { name: 'Déconnexion' }).click()

  // Clinic admin sees the invitation marked accepted.
  await page.goto('/login')
  await page.getByLabel('Adresse e-mail').fill('clinic-admin@tabibma.dev')
  await page.getByLabel('Mot de passe').fill('changeme-clinicadmin-dev-only')
  await page.getByRole('button', { name: 'Se connecter' }).click()
  await page.getByRole('link', { name: 'Ma clinique' }).click()
  await expect(invitationRow).toContainText('Acceptée')
})

test('a doctor cannot reach the clinic-admin dashboard', async ({ page }) => {
  const email = uniqueEmail('doctor-rbac')

  await page.goto('/register')
  await page.getByLabel('Prénom').fill('Hicham')
  await page.getByLabel('Nom', { exact: true }).fill('Tazi')
  await page.getByLabel('Adresse e-mail').fill(email)
  await page.getByLabel('Mot de passe').fill('Sup3rSecret!')
  await page.getByRole('combobox').click()
  await page.getByRole('option', { name: 'Médecin' }).click()
  await page.getByRole('button', { name: 'Créer mon compte' }).click()
  await expect(page).toHaveURL('/')

  await page.goto('/clinic-admin')
  await expect(page).toHaveURL('/')
})
