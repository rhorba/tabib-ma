import { test, expect } from '@playwright/test'

// Story 1.1 smoke suite — the only critical flows built so far (booking/video/prescription
// are later epics, out of scope here). Runs against the real backend, not MSW mocks.
function uniqueEmail(prefix: string) {
  return `${prefix}.${Date.now()}.${Math.floor(Math.random() * 1000)}@example.com`
}

test.describe('Auth — French (default locale)', () => {
  test('register a new patient and land authenticated on home', async ({ page }) => {
    const email = uniqueEmail('patiente')

    await page.goto('/register')
    await expect(page.getByRole('heading', { name: 'Créer un compte' })).toBeVisible()

    await page.getByLabel('Prénom').fill('Fatima')
    await page.getByLabel('Nom', { exact: true }).fill('Zahra')
    await page.getByLabel('Adresse e-mail').fill(email)
    await page.getByLabel('Mot de passe').fill('Sup3rSecret!')
    await page.getByRole('button', { name: 'Créer mon compte' }).click()

    await expect(page).toHaveURL('/')
    await expect(page.getByText('Bonjour, Fatima')).toBeVisible()
    await expect(page.getByRole('button', { name: 'Déconnexion' })).toBeVisible()
  })

  test('log out then log back in with the same credentials', async ({ page }) => {
    const email = uniqueEmail('patient')

    await page.goto('/register')
    await page.getByLabel('Prénom').fill('Youssef')
    await page.getByLabel('Nom', { exact: true }).fill('Amrani')
    await page.getByLabel('Adresse e-mail').fill(email)
    await page.getByLabel('Mot de passe').fill('Sup3rSecret!')
    await page.getByRole('button', { name: 'Créer mon compte' }).click()
    await expect(page.getByText('Bonjour, Youssef')).toBeVisible()

    await page.getByRole('button', { name: 'Déconnexion' }).click()
    await expect(page.getByRole('link', { name: 'Connexion' })).toBeVisible()

    await page.goto('/login')
    await page.getByLabel('Adresse e-mail').fill(email)
    await page.getByLabel('Mot de passe').fill('Sup3rSecret!')
    await page.getByRole('button', { name: 'Se connecter' }).click()

    await expect(page).toHaveURL('/')
    await expect(page.getByText('Bonjour, Youssef')).toBeVisible()
  })

  test('rejects login with the wrong password', async ({ page }) => {
    const email = uniqueEmail('wrongpass')

    await page.goto('/register')
    await page.getByLabel('Prénom').fill('Nadia')
    await page.getByLabel('Nom', { exact: true }).fill('Idrissi')
    await page.getByLabel('Adresse e-mail').fill(email)
    await page.getByLabel('Mot de passe').fill('Sup3rSecret!')
    await page.getByRole('button', { name: 'Créer mon compte' }).click()
    await expect(page.getByText('Bonjour, Nadia')).toBeVisible()
    await page.getByRole('button', { name: 'Déconnexion' }).click()

    await page.goto('/login')
    await page.getByLabel('Adresse e-mail').fill(email)
    await page.getByLabel('Mot de passe').fill('wrong-password')
    await page.getByRole('button', { name: 'Se connecter' }).click()

    await expect(page.getByText('E-mail ou mot de passe incorrect.')).toBeVisible()
  })
})

test.describe('Auth — Arabic (RTL)', () => {
  test('switch to Arabic flips document direction and translates the register form', async ({
    page,
  }) => {
    await page.goto('/register')
    await page.getByRole('button', { name: 'العربية' }).click()

    await expect(page.locator('html')).toHaveAttribute('dir', 'rtl')
    await expect(page.locator('html')).toHaveAttribute('lang', 'ar')
    await expect(page.getByRole('heading', { name: 'إنشاء حساب' })).toBeVisible()
  })

  test('register a patient end-to-end in Arabic', async ({ page }) => {
    const email = uniqueEmail('mareed')

    await page.goto('/register')
    await page.getByRole('button', { name: 'العربية' }).click()
    await expect(page.locator('html')).toHaveAttribute('dir', 'rtl')

    await page.getByLabel('الاسم الشخصي').fill('Amina')
    await page.getByLabel('الاسم العائلي').fill('Bennani')
    await page.getByLabel('البريد الإلكتروني').fill(email)
    await page.getByLabel('كلمة المرور').fill('Sup3rSecret!')
    await page.getByRole('button', { name: 'إنشاء الحساب' }).click()

    await expect(page).toHaveURL('/')
    await expect(page.getByText('مرحبًا، Amina')).toBeVisible()
  })
})
