import { test } from '@playwright/test'

const SCREENSHOT_DIR = '../docs/screenshots'

test.describe('Terminal & Mobile Mockup Screenshots', () => {
  // ── Terminal screens (Raspberry Pi Iced GUI mockups) ──────────────────

  test('Terminal - Idle Screen', async ({ page }) => {
    await page.setContent(`
      <div style="width:1024px;height:600px;background:#1a1a2e;display:flex;align-items:center;justify-content:center;font-family:'Segoe UI',sans-serif;color:#e0e0e0;">
        <div style="text-align:center;">
          <div style="font-size:28px;color:#b0b0b0;">Firma GmbH</div>
          <div style="height:30px;"></div>
          <div style="font-size:80px;font-weight:bold;color:#ffffff;">14:32:05</div>
          <div style="font-size:22px;color:#a0a0a0;">Samstag, 01. März 2026</div>
          <div style="height:50px;"></div>
          <div style="font-size:28px;color:#c0c0c0;">Bitte scannen Sie Ihren Ausweis</div>
        </div>
      </div>
    `)
    await page.setViewportSize({ width: 1024, height: 600 })
    await page.screenshot({ path: `${SCREENSHOT_DIR}/terminal-idle.png` })
  })

  test('Terminal - Clock In Screen', async ({ page }) => {
    await page.setContent(`
      <div style="width:1024px;height:600px;background:#0d260d;display:flex;align-items:center;justify-content:center;font-family:'Segoe UI',sans-serif;color:#e0e0e0;">
        <div style="text-align:center;">
          <div style="font-size:48px;color:#33e64d;">✓  Eingestempelt</div>
          <div style="height:30px;"></div>
          <div style="font-size:36px;color:#ffffff;">Max Mustermann</div>
          <div style="height:10px;"></div>
          <div style="font-size:24px;color:#c0c0c0;">Uhrzeit: 08:02:15</div>
          <div style="height:40px;"></div>
          <div style="font-size:18px;color:#999;">Zurück in 7s</div>
        </div>
      </div>
    `)
    await page.setViewportSize({ width: 1024, height: 600 })
    await page.screenshot({ path: `${SCREENSHOT_DIR}/terminal-clock-in.png` })
  })

  test('Terminal - Clock Out Screen', async ({ page }) => {
    await page.setContent(`
      <div style="width:1024px;height:600px;background:#260808;display:flex;align-items:center;justify-content:center;font-family:'Segoe UI',sans-serif;color:#e0e0e0;">
        <div style="text-align:center;">
          <div style="font-size:48px;color:#f23333;">✗  Ausgestempelt</div>
          <div style="height:30px;"></div>
          <div style="font-size:36px;color:#ffffff;">Max Mustermann</div>
          <div style="height:10px;"></div>
          <div style="font-size:24px;color:#c0c0c0;">Uhrzeit: 16:35:42</div>
          <div style="height:30px;"></div>
          <div style="display:flex;justify-content:center;gap:60px;">
            <div style="text-align:center;"><div style="font-size:16px;color:#999;">Arbeitszeit</div><div style="font-size:28px;color:#fff;">8h 33min</div></div>
            <div style="text-align:center;"><div style="font-size:16px;color:#999;">Pause</div><div style="font-size:28px;color:#fff;">30min</div></div>
          </div>
          <div style="height:10px;"></div>
          <div style="display:flex;justify-content:center;gap:60px;">
            <div style="text-align:center;"><div style="font-size:16px;color:#999;">Überstunden</div><div style="font-size:28px;color:#fff;">+0h 33min</div></div>
            <div style="text-align:center;"><div style="font-size:16px;color:#999;">Resturlaub</div><div style="font-size:28px;color:#fff;">22.0 Tage</div></div>
          </div>
          <div style="height:30px;"></div>
          <div style="font-size:18px;color:#999;">Zurück in 6s</div>
        </div>
      </div>
    `)
    await page.setViewportSize({ width: 1024, height: 600 })
    await page.screenshot({ path: `${SCREENSHOT_DIR}/terminal-clock-out.png` })
  })

  test('Terminal - Error Screen', async ({ page }) => {
    await page.setContent(`
      <div style="width:1024px;height:600px;background:#261a00;display:flex;align-items:center;justify-content:center;font-family:'Segoe UI',sans-serif;color:#e0e0e0;">
        <div style="text-align:center;">
          <div style="font-size:42px;color:#ffa600;">⚠  Ausweis nicht erkannt</div>
          <div style="height:20px;"></div>
          <div style="font-size:24px;color:#c0c0c0;">Dieser Ausweis ist nicht registriert.</div>
          <div style="height:10px;"></div>
          <div style="font-size:18px;color:#999;">Bitte kontaktieren Sie Ihren Administrator.</div>
          <div style="height:40px;"></div>
          <div style="font-size:18px;color:#999;">Zurück in 4s</div>
        </div>
      </div>
    `)
    await page.setViewportSize({ width: 1024, height: 600 })
    await page.screenshot({ path: `${SCREENSHOT_DIR}/terminal-error.png` })
  })

  test('Terminal - Offline Screen', async ({ page }) => {
    await page.setContent(`
      <div style="width:1024px;height:600px;background:#261a00;display:flex;align-items:center;justify-content:center;font-family:'Segoe UI',sans-serif;color:#e0e0e0;">
        <div style="text-align:center;">
          <div style="font-size:42px;color:#ffa600;">↑  Offline gespeichert</div>
          <div style="height:20px;"></div>
          <div style="font-size:24px;color:#c0c0c0;">Der Scan wurde lokal gespeichert</div>
          <div style="font-size:24px;color:#c0c0c0;">und beim nächsten Start synchronisiert.</div>
          <div style="height:20px;"></div>
          <div style="font-size:20px;color:#c0c0c0;">Uhrzeit: 08:15:30</div>
          <div style="height:40px;"></div>
          <div style="font-size:18px;color:#999;">Zurück in 5s</div>
        </div>
      </div>
    `)
    await page.setViewportSize({ width: 1024, height: 600 })
    await page.screenshot({ path: `${SCREENSHOT_DIR}/terminal-offline.png` })
  })

  // ── Mobile App screens (Android/iOS mockups) ─────────────────────────

  test('Mobile - Login Screen', async ({ page }) => {
    await page.setContent(`
      <div style="width:390px;height:844px;background:#f5f5f5;font-family:-apple-system,'Segoe UI',sans-serif;overflow:hidden;">
        <div style="height:50px;background:#1a56db;display:flex;align-items:center;justify-content:center;">
          <span style="color:white;font-size:18px;font-weight:600;">Zeiterfassung</span>
        </div>
        <div style="padding:40px 24px;display:flex;flex-direction:column;align-items:center;">
          <div style="height:60px;"></div>
          <div style="font-size:28px;font-weight:700;color:#1a1a1a;">Anmelden</div>
          <div style="height:8px;"></div>
          <div style="font-size:14px;color:#666;">Geben Sie Ihre Zugangsdaten ein</div>
          <div style="height:32px;"></div>
          <div style="width:100%;">
            <label style="font-size:14px;color:#333;font-weight:500;">E-Mail</label>
            <div style="margin-top:6px;padding:12px 16px;border:1px solid #d0d0d0;border-radius:10px;background:white;font-size:16px;color:#999;">max@example.com</div>
          </div>
          <div style="height:16px;"></div>
          <div style="width:100%;">
            <label style="font-size:14px;color:#333;font-weight:500;">Passwort</label>
            <div style="margin-top:6px;padding:12px 16px;border:1px solid #d0d0d0;border-radius:10px;background:white;font-size:16px;color:#999;">••••••••</div>
          </div>
          <div style="height:24px;"></div>
          <div style="width:100%;padding:14px;background:#1a56db;border-radius:10px;text-align:center;color:white;font-size:16px;font-weight:600;">Anmelden</div>
          <div style="height:16px;"></div>
          <div style="font-size:14px;color:#1a56db;">Passwort vergessen?</div>
        </div>
      </div>
    `)
    await page.setViewportSize({ width: 390, height: 844 })
    await page.screenshot({ path: `${SCREENSHOT_DIR}/mobile-login.png` })
  })

  test('Mobile - Dashboard Screen', async ({ page }) => {
    await page.setContent(`
      <div style="width:390px;height:844px;background:#f5f5f5;font-family:-apple-system,'Segoe UI',sans-serif;overflow:hidden;">
        <div style="height:50px;background:#1a56db;display:flex;align-items:center;justify-content:center;">
          <span style="color:white;font-size:18px;font-weight:600;">Dashboard</span>
        </div>
        <div style="padding:16px;">
          <div style="background:white;border-radius:12px;padding:16px;margin-bottom:12px;box-shadow:0 1px 3px rgba(0,0,0,0.1);">
            <div style="font-size:14px;color:#666;">Hallo, Max</div>
            <div style="font-size:20px;font-weight:600;color:#1a1a1a;">Willkommen zurück</div>
          </div>
          <div style="display:grid;grid-template-columns:1fr 1fr;gap:12px;margin-bottom:12px;">
            <div style="background:white;border-radius:12px;padding:16px;box-shadow:0 1px 3px rgba(0,0,0,0.1);">
              <div style="font-size:12px;color:#666;">Heute</div>
              <div style="font-size:24px;font-weight:700;color:#1a56db;">6:45</div>
              <div style="font-size:11px;color:#999;">Stunden</div>
            </div>
            <div style="background:white;border-radius:12px;padding:16px;box-shadow:0 1px 3px rgba(0,0,0,0.1);">
              <div style="font-size:12px;color:#666;">Diese Woche</div>
              <div style="font-size:24px;font-weight:700;color:#1a56db;">32:15</div>
              <div style="font-size:11px;color:#999;">Stunden</div>
            </div>
            <div style="background:white;border-radius:12px;padding:16px;box-shadow:0 1px 3px rgba(0,0,0,0.1);">
              <div style="font-size:12px;color:#666;">Resturlaub</div>
              <div style="font-size:24px;font-weight:700;color:#16a34a;">22</div>
              <div style="font-size:11px;color:#999;">Tage</div>
            </div>
            <div style="background:white;border-radius:12px;padding:16px;box-shadow:0 1px 3px rgba(0,0,0,0.1);">
              <div style="font-size:12px;color:#666;">Überstunden</div>
              <div style="font-size:24px;font-weight:700;color:#ea580c;">+2:15</div>
              <div style="font-size:11px;color:#999;">Stunden</div>
            </div>
          </div>
          <div style="background:#1a56db;border-radius:12px;padding:20px;text-align:center;color:white;margin-bottom:12px;">
            <div style="font-size:14px;opacity:0.8;">Status</div>
            <div style="font-size:22px;font-weight:700;margin:8px 0;">● Eingestempelt</div>
            <div style="font-size:13px;opacity:0.7;">seit 08:02</div>
          </div>
        </div>
        <div style="position:absolute;bottom:0;left:0;right:0;height:60px;background:white;display:flex;border-top:1px solid #e0e0e0;">
          <div style="flex:1;display:flex;flex-direction:column;align-items:center;justify-content:center;color:#1a56db;font-size:11px;font-weight:600;">
            <span style="font-size:20px;">🏠</span>Dashboard
          </div>
          <div style="flex:1;display:flex;flex-direction:column;align-items:center;justify-content:center;color:#999;font-size:11px;">
            <span style="font-size:20px;">⏱</span>Zeit
          </div>
          <div style="flex:1;display:flex;flex-direction:column;align-items:center;justify-content:center;color:#999;font-size:11px;">
            <span style="font-size:20px;">🏖</span>Urlaub
          </div>
          <div style="flex:1;display:flex;flex-direction:column;align-items:center;justify-content:center;color:#999;font-size:11px;">
            <span style="font-size:20px;">⚙</span>Mehr
          </div>
        </div>
      </div>
    `)
    await page.setViewportSize({ width: 390, height: 844 })
    await page.screenshot({ path: `${SCREENSHOT_DIR}/mobile-dashboard.png` })
  })

  test('Mobile - Time Tracking Screen', async ({ page }) => {
    await page.setContent(`
      <div style="width:390px;height:844px;background:#f5f5f5;font-family:-apple-system,'Segoe UI',sans-serif;overflow:hidden;">
        <div style="height:50px;background:#1a56db;display:flex;align-items:center;justify-content:center;">
          <span style="color:white;font-size:18px;font-weight:600;">Zeiterfassung</span>
        </div>
        <div style="padding:16px;">
          <div style="background:white;border-radius:12px;padding:24px;text-align:center;margin-bottom:16px;box-shadow:0 1px 3px rgba(0,0,0,0.1);">
            <div style="font-size:14px;color:#666;">Aktuelle Arbeitszeit</div>
            <div style="font-size:48px;font-weight:700;color:#1a56db;margin:12px 0;">06:45</div>
            <div style="font-size:13px;color:#999;">Eingestempelt seit 08:02</div>
          </div>
          <div style="display:grid;grid-template-columns:1fr 1fr;gap:12px;margin-bottom:16px;">
            <div style="background:#dc2626;border-radius:12px;padding:16px;text-align:center;color:white;font-weight:600;font-size:15px;">Ausstempeln</div>
            <div style="background:#f59e0b;border-radius:12px;padding:16px;text-align:center;color:white;font-weight:600;font-size:15px;">Pause starten</div>
          </div>
          <div style="background:white;border-radius:12px;padding:16px;box-shadow:0 1px 3px rgba(0,0,0,0.1);">
            <div style="font-size:16px;font-weight:600;color:#1a1a1a;margin-bottom:12px;">Heutige Einträge</div>
            <div style="display:flex;justify-content:space-between;padding:8px 0;border-bottom:1px solid #f0f0f0;">
              <span style="color:#16a34a;font-weight:500;">● Einstempeln</span><span style="color:#666;">08:02</span>
            </div>
            <div style="display:flex;justify-content:space-between;padding:8px 0;border-bottom:1px solid #f0f0f0;">
              <span style="color:#f59e0b;font-weight:500;">● Pause Start</span><span style="color:#666;">12:00</span>
            </div>
            <div style="display:flex;justify-content:space-between;padding:8px 0;border-bottom:1px solid #f0f0f0;">
              <span style="color:#f59e0b;font-weight:500;">● Pause Ende</span><span style="color:#666;">12:30</span>
            </div>
          </div>
        </div>
        <div style="position:absolute;bottom:0;left:0;right:0;height:60px;background:white;display:flex;border-top:1px solid #e0e0e0;">
          <div style="flex:1;display:flex;flex-direction:column;align-items:center;justify-content:center;color:#999;font-size:11px;">
            <span style="font-size:20px;">🏠</span>Dashboard
          </div>
          <div style="flex:1;display:flex;flex-direction:column;align-items:center;justify-content:center;color:#1a56db;font-size:11px;font-weight:600;">
            <span style="font-size:20px;">⏱</span>Zeit
          </div>
          <div style="flex:1;display:flex;flex-direction:column;align-items:center;justify-content:center;color:#999;font-size:11px;">
            <span style="font-size:20px;">🏖</span>Urlaub
          </div>
          <div style="flex:1;display:flex;flex-direction:column;align-items:center;justify-content:center;color:#999;font-size:11px;">
            <span style="font-size:20px;">⚙</span>Mehr
          </div>
        </div>
      </div>
    `)
    await page.setViewportSize({ width: 390, height: 844 })
    await page.screenshot({ path: `${SCREENSHOT_DIR}/mobile-time-tracking.png` })
  })

  test('Mobile - Vacation Screen', async ({ page }) => {
    await page.setContent(`
      <div style="width:390px;height:844px;background:#f5f5f5;font-family:-apple-system,'Segoe UI',sans-serif;overflow:hidden;">
        <div style="height:50px;background:#1a56db;display:flex;align-items:center;justify-content:center;">
          <span style="color:white;font-size:18px;font-weight:600;">Urlaub</span>
        </div>
        <div style="padding:16px;">
          <div style="background:white;border-radius:12px;padding:16px;margin-bottom:12px;box-shadow:0 1px 3px rgba(0,0,0,0.1);">
            <div style="font-size:16px;font-weight:600;color:#1a1a1a;margin-bottom:12px;">Urlaubskonto 2026</div>
            <div style="display:flex;justify-content:space-between;margin-bottom:8px;">
              <span style="color:#666;font-size:14px;">Jahresanspruch</span><span style="font-weight:600;">30 Tage</span>
            </div>
            <div style="display:flex;justify-content:space-between;margin-bottom:8px;">
              <span style="color:#666;font-size:14px;">Genommen</span><span style="font-weight:600;color:#dc2626;">-8 Tage</span>
            </div>
            <div style="display:flex;justify-content:space-between;padding-top:8px;border-top:1px solid #f0f0f0;">
              <span style="color:#666;font-size:14px;font-weight:600;">Verbleibend</span><span style="font-weight:700;color:#16a34a;font-size:18px;">22 Tage</span>
            </div>
          </div>
          <div style="background:#1a56db;border-radius:12px;padding:14px;text-align:center;color:white;font-weight:600;margin-bottom:16px;">+ Neuen Antrag stellen</div>
          <div style="font-size:16px;font-weight:600;color:#1a1a1a;margin-bottom:12px;">Meine Anträge</div>
          <div style="background:white;border-radius:12px;padding:14px;margin-bottom:8px;box-shadow:0 1px 3px rgba(0,0,0,0.1);">
            <div style="display:flex;justify-content:space-between;align-items:center;">
              <div><div style="font-weight:600;font-size:14px;">01.06. — 05.06.2026</div><div style="font-size:12px;color:#666;">5 Tage · Sommerurlaub</div></div>
              <span style="background:#dcfce7;color:#16a34a;padding:4px 10px;border-radius:20px;font-size:12px;font-weight:600;">Genehmigt</span>
            </div>
          </div>
          <div style="background:white;border-radius:12px;padding:14px;margin-bottom:8px;box-shadow:0 1px 3px rgba(0,0,0,0.1);">
            <div style="display:flex;justify-content:space-between;align-items:center;">
              <div><div style="font-weight:600;font-size:14px;">14.07. — 18.07.2026</div><div style="font-size:12px;color:#666;">4.5 Tage</div></div>
              <span style="background:#fef9c3;color:#a16207;padding:4px 10px;border-radius:20px;font-size:12px;font-weight:600;">Ausstehend</span>
            </div>
          </div>
        </div>
        <div style="position:absolute;bottom:0;left:0;right:0;height:60px;background:white;display:flex;border-top:1px solid #e0e0e0;">
          <div style="flex:1;display:flex;flex-direction:column;align-items:center;justify-content:center;color:#999;font-size:11px;">
            <span style="font-size:20px;">🏠</span>Dashboard
          </div>
          <div style="flex:1;display:flex;flex-direction:column;align-items:center;justify-content:center;color:#999;font-size:11px;">
            <span style="font-size:20px;">⏱</span>Zeit
          </div>
          <div style="flex:1;display:flex;flex-direction:column;align-items:center;justify-content:center;color:#1a56db;font-size:11px;font-weight:600;">
            <span style="font-size:20px;">🏖</span>Urlaub
          </div>
          <div style="flex:1;display:flex;flex-direction:column;align-items:center;justify-content:center;color:#999;font-size:11px;">
            <span style="font-size:20px;">⚙</span>Mehr
          </div>
        </div>
      </div>
    `)
    await page.setViewportSize({ width: 390, height: 844 })
    await page.screenshot({ path: `${SCREENSHOT_DIR}/mobile-vacation.png` })
  })
})
