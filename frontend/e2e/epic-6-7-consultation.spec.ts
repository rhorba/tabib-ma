import { test, expect, type Page } from '@playwright/test'

// Epic 6+7 e2e (Stories 6.1, 6.3, 7.1, 7.2) — runs against the real backend,
// including a genuine two-peer WebRTC connection over the backend's signaling
// relay (not mocked), mirroring epic-4-booking.spec.ts's two-independent-
// browser-context pattern. Specialties are unique per run for the same reason
// as the other specs: result-count/queue-item assertions must not be affected
// by prior runs' accumulated data in the persistent dev DB.
const PASSWORD = 'Sup3rSecret!'
const WEEKDAYS = ['SUNDAY', 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY']

function unique(prefix: string) {
  return `${prefix}-${Date.now()}-${Math.floor(Math.random() * 1000)}`
}

function uniqueEmail(prefix: string) {
  return `${prefix}.${Date.now()}.${Math.floor(Math.random() * 1000)}@example.com`
}

function pad(n: number) {
  return String(n).padStart(2, '0')
}

function casablancaParts() {
  return new Intl.DateTimeFormat('en-US', {
    timeZone: 'Africa/Casablanca',
    weekday: 'long',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).formatToParts(new Date())
}

// AvailabilityService.CLINIC_ZONE is Africa/Casablanca. The slot must start a
// few minutes *after* "now" here, not before: SlotPicker's own open-slots
// query uses `from = now` computed when the patient's booking page loads
// (later, after doctor setup + admin approval), and a slot whose start has
// already passed by then silently drops out of the bookable list even though
// JoinWindowPolicy's +/-10min margin would still call it joinable — that's
// exactly the bug this comment is here to stop someone reintroducing. +5min
// stays comfortably inside the join window (which opens 10min before start)
// for the whole test. Clamped away from both midnight edges so the generated
// slot's weekday always matches the one used for the select.
function computeTodayVideoWindow() {
  const parts = casablancaParts()
  const get = (type: string) => parts.find((p) => p.type === type)!.value
  const weekday = get('weekday').toUpperCase()
  const nowMinutes = (Number(get('hour')) % 24) * 60 + Number(get('minute'))
  const startMinutes = Math.max(0, Math.min(nowMinutes + 5, 24 * 60 - 35))
  const endMinutes = startMinutes + 30
  const fmt = (total: number) => `${pad(Math.floor(total / 60))}:${pad(total % 60)}`
  return { weekday, startTime: fmt(startMinutes), endTime: fmt(endMinutes) }
}

// The next occurrence of "tomorrow's weekday" starting a 30-day generation
// from today is always exactly 1 calendar day out — deterministically far
// outside any join window regardless of what time the suite happens to run.
function tomorrowsWeekdayInCasablanca() {
  const today = casablancaParts()
    .find((p) => p.type === 'weekday')!
    .value.toUpperCase()
  return WEEKDAYS[(WEEKDAYS.indexOf(today) + 1) % 7]
}

async function registerDoctor(page: Page, specialty: string) {
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
  await page.getByLabel('Tarif de consultation (MAD)').fill('300')
  await page.getByRole('button', { name: 'Créer mon profil' }).click()
  await expect(page.getByText('En attente de vérification')).toBeVisible()
  return email
}

// Shadcn's Select renders a visually-hidden native <select> for form semantics
// alongside the actual Radix trigger button the <label> points to — so
// getByLabel(...).selectOption() resolves to the button and fails. Interact
// with it the way a user would instead: click the trigger, click the option.
async function selectRadixOption(page: Page, labelText: string, optionText: string) {
  await page.getByLabel(labelText).click()
  await page.getByRole('option', { name: optionText, exact: true }).click()
}

const WEEKDAY_LABELS: Record<string, string> = {
  MONDAY: 'Lundi',
  TUESDAY: 'Mardi',
  WEDNESDAY: 'Mercredi',
  THURSDAY: 'Jeudi',
  FRIDAY: 'Vendredi',
  SATURDAY: 'Samedi',
  SUNDAY: 'Dimanche',
}

async function addVideoRuleAndGenerate(
  page: Page,
  window: { weekday: string; startTime: string; endTime: string }
) {
  await page.getByRole('link', { name: 'Mes disponibilités' }).click()
  await selectRadixOption(page, 'Jour de la semaine', WEEKDAY_LABELS[window.weekday])
  await page.getByLabel('Heure de début').fill(window.startTime)
  await page.getByLabel('Heure de fin').fill(window.endTime)
  await selectRadixOption(page, 'Type de consultation', 'Téléconsultation')
  await page.getByRole('button', { name: 'Ajouter ce créneau récurrent' }).click()
  await expect(page.getByText(new RegExp(`${window.startTime}.${window.endTime}`))).toBeVisible()
  await page.getByRole('button', { name: 'Générer les créneaux des 30 prochains jours' }).click()
  await expect(page.getByText(/créneaux? générés?/)).toBeVisible()
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

async function login(page: Page, email: string, password: string) {
  await page.goto('/login')
  await page.getByLabel('Adresse e-mail').fill(email)
  await page.getByLabel('Mot de passe').fill(password)
  await page.getByRole('button', { name: 'Se connecter' }).click()
  // Without this, a subsequent direct page.goto() can race AuthContext's
  // token-bootstrap and land on the target route still logged out.
  await expect(page).toHaveURL('/')
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

async function bookFirstSlot(page: Page, specialty: string) {
  await page.getByRole('link', { name: 'Rechercher un médecin' }).click()
  await page.getByLabel('Spécialité').fill(specialty)
  await page.getByRole('button', { name: 'Rechercher' }).click()
  await page.getByRole('link', { name: 'Voir le profil' }).click()
  await page.getByRole('link', { name: 'Prendre rendez-vous' }).click()
  const slotButton = page.getByRole('button', { name: /^\d{1,2}\/\d{1,2}\/\d{4}/ }).first()
  await expect(slotButton).toBeVisible()
  await slotButton.click()

  const [bookingResponse] = await Promise.all([
    page.waitForResponse(
      (r) => r.url().includes('/api/v1/booking/appointments') && r.request().method() === 'POST'
    ),
    page.getByRole('button', { name: 'Confirmer et payer' }).click(),
  ])
  await expect(page.getByText('Votre rendez-vous est confirmé.')).toBeVisible()
  return (await bookingResponse.json()) as { id: string }
}

test.describe('Epic 6+7: video consultation + prescriptions', () => {
  test('doctor and patient join a video consultation, connect, and the doctor completes it with a prescription the patient can then download', async ({
    browser,
  }, testInfo) => {
    test.setTimeout(120_000)
    const specialty = unique('Neurologie')
    const recordVideo = { dir: testInfo.outputPath('video') }

    // Sequential setup on one throwaway context — same convention as
    // epic-4-booking.spec.ts's setUpDoctorWithSlots + approveDoctor.
    const setupPage = await (await browser.newContext({ recordVideo })).newPage()
    const doctorEmail = await registerDoctor(setupPage, specialty)
    await addVideoRuleAndGenerate(setupPage, computeTodayVideoWindow())
    await setupPage.getByRole('button', { name: 'Déconnexion' }).click()
    await approveDoctor(setupPage, specialty)
    await setupPage.close()

    // Two independent, persistent contexts — this is what actually lets two
    // real browser peers negotiate WebRTC with each other, not a UI-only detail.
    const doctorContext = await browser.newContext({ recordVideo })
    const patientContext = await browser.newContext({ recordVideo })
    const doctorPage = await doctorContext.newPage()
    const patientPage = await patientContext.newPage()

    await login(doctorPage, doctorEmail, PASSWORD)
    await registerPatient(patientPage, 'Youssef')
    const appointment = await bookFirstSlot(patientPage, specialty)

    // Doctor has no appointments dashboard yet (flagged as a fast-follow when
    // Batch 4 built ConsultationPage) — reach it directly with the id captured
    // from the booking response, exactly as planned for this e2e batch.
    await doctorPage.goto(`/appointments/${appointment.id}/consultation`)
    await doctorPage.getByRole('button', { name: 'Rejoindre la consultation vidéo' }).click()

    await patientPage.getByRole('link', { name: 'Voir mes rendez-vous' }).click()
    await patientPage.getByRole('link', { name: 'Rejoindre la vidéo' }).click()
    await patientPage.getByRole('button', { name: 'Rejoindre la consultation vidéo' }).click()

    // Real WebRTC negotiation over the backend's signaling relay — generous
    // timeout. "Connexion en cours..." only renders before the peer connects.
    await expect(doctorPage.getByText('Connexion en cours…')).toBeHidden({ timeout: 45_000 })
    await expect(patientPage.getByText('Connexion en cours…')).toBeHidden({ timeout: 45_000 })

    // The prescription form only renders once the doctor's own peer connection
    // is actually 'connected' — this is the real proof the two sides linked up.
    await expect(
      doctorPage.getByRole('heading', { name: 'Terminer la consultation et prescrire' })
    ).toBeVisible()

    await doctorPage.getByLabel('Médicament').fill('Paracétamol')
    await doctorPage.getByLabel('Posologie').fill('500mg, 3x/jour')
    await doctorPage.getByRole('button', { name: "Terminer et envoyer l'ordonnance" }).click()
    await expect(doctorPage.getByText('Consultation terminée et ordonnance envoyée.')).toBeVisible()

    await patientPage.getByRole('link', { name: 'Mes ordonnances' }).click()
    await expect(patientPage.getByText('Paracétamol')).toBeVisible()
    await expect(patientPage.getByText(/500mg, 3x\/jour/)).toBeVisible()

    const [download] = await Promise.all([
      patientPage.waitForEvent('download'),
      patientPage.getByRole('button', { name: 'Télécharger le PDF' }).click(),
    ])
    expect(download.suggestedFilename()).toBe('prescription.pdf')

    await doctorContext.close()
    await patientContext.close()
  })

  test('a patient cannot join a video consultation before the join window opens', async ({ page }) => {
    const specialty = unique('Psychiatrie')
    await registerDoctor(page, specialty)
    await addVideoRuleAndGenerate(page, {
      weekday: tomorrowsWeekdayInCasablanca(),
      startTime: '09:00',
      endTime: '09:30',
    })
    await page.getByRole('button', { name: 'Déconnexion' }).click()
    await approveDoctor(page, specialty)

    await registerPatient(page, 'Fatima')
    const appointment = await bookFirstSlot(page, specialty)
    await page.getByRole('link', { name: 'Voir mes rendez-vous' }).click()
    await page.getByRole('link', { name: 'Rejoindre la vidéo' }).click()
    await expect(page).toHaveURL(`/appointments/${appointment.id}/consultation`)

    await expect(page.getByText("La salle vidéo n'est pas disponible actuellement.")).toBeVisible()
    await expect(page.getByRole('button', { name: 'Rejoindre la consultation vidéo' })).not.toBeVisible()
  })
})
