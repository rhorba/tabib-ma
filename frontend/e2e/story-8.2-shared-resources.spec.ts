import { test, expect, type Page } from '@playwright/test'

// Story 8.2 smoke suite (shared clinic resources — the AC's actual conflict
// scenario) — runs against the real backend, not MSW mocks. Two doctors on
// the SAME clinic both require the SAME room for an overlapping time window;
// the first patient's booking succeeds, the second must be rejected by
// ResourceAllocationGuard's all-or-nothing EXCLUDE constraint even though the
// two doctors' AvailabilitySlot rows are entirely distinct (this is not a
// DoubleBookingGuard case — same lesson as Epic 4 Batch 8's slot-race test,
// but one level up, at the resource layer). Reuses the seeded CLINIC_ADMIN
// from V4__clinic_invitations.sql (same account as epic-8-clinic-dashboard's
// suite), so the clinic and its resource list accumulate across runs — every
// name used here (specialty, resource) is unique per run so the doctor's
// resource picker and the search results stay unambiguous regardless of
// leftover data from prior runs.
const PASSWORD = 'Sup3rSecret!'
const CLINIC_ADMIN_EMAIL = 'clinic-admin@tabibma.dev'
const CLINIC_ADMIN_PASSWORD = 'changeme-clinicadmin-dev-only'
const PLATFORM_ADMIN_EMAIL = 'tabib-admin@tabibma.dev'
const PLATFORM_ADMIN_PASSWORD = 'changeme-admin-dev-only'
const RULE_START_TIME = '10:00'
const RULE_END_TIME = '10:30'
const FR_WEEKDAY_LABELS = ['Dimanche', 'Lundi', 'Mardi', 'Mercredi', 'Jeudi', 'Vendredi', 'Samedi']

function unique(prefix: string) {
  return `${prefix}-${Date.now()}-${Math.floor(Math.random() * 1000)}`
}

function uniqueEmail(prefix: string) {
  return `${prefix}.${Date.now()}.${Math.floor(Math.random() * 1000)}@example.com`
}

// Strictly tomorrow (not "today or later") — same reasoning as the
// ResourceUtilizationControllerIntegrationTest fix from this same session: a
// fixed clock-time window on "today" can already be in the past by the time
// this runs, depending on what time of day the suite happens to execute.
function tomorrowWeekdayLabel() {
  const tomorrow = new Date(Date.now() + 24 * 60 * 60 * 1000)
  return FR_WEEKDAY_LABELS[tomorrow.getDay()]
}

async function registerDoctorWithProfile(page: Page, specialty: string) {
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
  await page.getByLabel('Tarif de consultation (MAD)').fill('250')
  await page.getByRole('button', { name: 'Créer mon profil' }).click()
  await expect(page.getByText('En attente de vérification')).toBeVisible()

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

/** Clinic admin: ensures the seeded clinic exists, adds a fresh uniquely-named
 * ROOM resource, and invites both doctors. Returns the resource's name so the
 * doctor-side setup can select it unambiguously among any resources left over
 * from prior runs. */
async function setUpClinicResourceAndInvite(page: Page, doctorEmails: string[]) {
  await page.goto('/login')
  await page.getByLabel('Adresse e-mail').fill(CLINIC_ADMIN_EMAIL)
  await page.getByLabel('Mot de passe').fill(CLINIC_ADMIN_PASSWORD)
  await page.getByRole('button', { name: 'Se connecter' }).click()
  await expect(page).toHaveURL('/')

  await page.getByRole('link', { name: 'Ma clinique' }).click()
  const clinicNameInput = page.getByLabel('Nom de la clinique')
  if (await clinicNameInput.isVisible().catch(() => false)) {
    await clinicNameInput.fill('Clinique Al Amal E2E')
    await page.getByLabel('Ville').fill('Rabat')
    await page.getByRole('button', { name: 'Créer ma clinique' }).click()
  }

  const resourceName = unique('Salle Partagée')
  await expect(page.getByRole('button', { name: 'Ajouter la ressource' })).toBeVisible()
  await page.getByLabel('Nom', { exact: true }).fill(resourceName)
  await page.getByRole('button', { name: 'Ajouter la ressource' }).click()
  await expect(page.getByText(resourceName)).toBeVisible()

  for (const email of doctorEmails) {
    await page.getByLabel('Adresse e-mail du médecin').fill(email)
    await page.getByRole('button', { name: 'Inviter' }).click()
    await expect(page.getByRole('listitem').filter({ hasText: email })).toContainText('En attente')
  }

  await page.getByRole('button', { name: 'Déconnexion' }).click()
  return resourceName
}

/** Doctor: accepts the pending clinic invitation, then creates an IN_PERSON
 * availability rule requiring the shared resource on tomorrow's weekday
 * (same window for every doctor calling this, so their slots genuinely
 * overlap), and generates slots. */
async function acceptInviteAndCreateResourceScopedRule(
  page: Page,
  email: string,
  resourceName: string,
  dayLabel: string
) {
  await page.goto('/login')
  await page.getByLabel('Adresse e-mail').fill(email)
  await page.getByLabel('Mot de passe').fill(PASSWORD)
  await page.getByRole('button', { name: 'Se connecter' }).click()

  await page.getByRole('link', { name: 'Mon profil médecin' }).click()
  await page.getByRole('button', { name: 'Accepter' }).click()
  await expect(page.getByText('Invitations de clinique en attente')).not.toBeVisible()

  await page.getByRole('link', { name: 'Mes disponibilités' }).click()
  await page.getByLabel('Jour de la semaine').click()
  await page.getByRole('option', { name: dayLabel }).click()
  await page.getByLabel('Heure de début').fill(RULE_START_TIME)
  await page.getByLabel('Heure de fin').fill(RULE_END_TIME)
  // locationType defaults to IN_PERSON, which is what reveals the clinic
  // picker — a doctor with exactly one clinic membership (this suite only
  // ever invites via the one seeded clinic admin) sees exactly two options:
  // the "no clinic" sentinel (always first) and the real clinic (last).
  await page.getByLabel('Clinique').click()
  await page.getByRole('option').last().click()
  await page.getByLabel(resourceName).click()
  await page.getByRole('button', { name: 'Ajouter ce créneau récurrent' }).click()
  await expect(
    page.getByText(`${dayLabel} · ${RULE_START_TIME}–${RULE_END_TIME}`)
  ).toBeVisible()

  await page.getByRole('button', { name: 'Générer les créneaux des 30 prochains jours' }).click()
  await expect(page.getByText(/créneaux générés/)).toBeVisible()

  await page.getByRole('button', { name: 'Déconnexion' }).click()
}

test('a second doctor cannot book the same shared room for an overlapping slot', async ({ page }) => {
  // Two full doctor onboarding + rule-setup flows plus two full booking flows
  // in one sequential test, well beyond the 30s default (matches the
  // precedent set by epic-6-7-consultation.spec.ts's own two-doctor setup).
  test.setTimeout(120_000)
  const specialtyA = unique('Ostéopathie')
  const specialtyB = unique('Podologie')
  const dayLabel = tomorrowWeekdayLabel()

  const doctorAEmail = await registerDoctorWithProfile(page, specialtyA)
  const doctorBEmail = await registerDoctorWithProfile(page, specialtyB)

  const resourceName = await setUpClinicResourceAndInvite(page, [doctorAEmail, doctorBEmail])

  await acceptInviteAndCreateResourceScopedRule(page, doctorAEmail, resourceName, dayLabel)
  await acceptInviteAndCreateResourceScopedRule(page, doctorBEmail, resourceName, dayLabel)

  await approveDoctor(page, specialtyA)
  await approveDoctor(page, specialtyB)

  await registerPatient(page, 'Karim')
  await searchAndOpenProfile(page, specialtyA)
  await page.getByRole('link', { name: 'Prendre rendez-vous' }).click()
  await page.getByRole('button', { name: /^\d{1,2}\/\d{1,2}\/\d{4}/ }).first().click()
  await page.getByRole('button', { name: 'Confirmer et payer' }).click()
  await expect(page.getByText('Votre rendez-vous est confirmé.')).toBeVisible()
  await page.getByRole('button', { name: 'Déconnexion' }).click()

  // Doctor B's slot is a completely different AvailabilitySlot row, at the
  // same wall-clock window, requiring the same shared room — only the
  // resource-allocation EXCLUDE constraint (not DoubleBookingGuard) can
  // catch this.
  await registerPatient(page, 'Sara')
  await searchAndOpenProfile(page, specialtyB)
  await page.getByRole('link', { name: 'Prendre rendez-vous' }).click()
  await page.getByRole('button', { name: /^\d{1,2}\/\d{1,2}\/\d{4}/ }).first().click()
  await page.getByRole('button', { name: 'Confirmer et payer' }).click()
  await expect(page.getByText("vient d'être réservé")).toBeVisible()
  await expect(page.getByText('Votre rendez-vous est confirmé.')).not.toBeVisible()
})
