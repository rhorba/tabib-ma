// Concatenates the per-test .webm files Playwright writes under test-results/
// into a single recording at root .recordings/v[version]-[date].webm, per
// CLAUDE.md rule 9. Requires ffmpeg on PATH (verified present on this machine).
import { execFileSync } from 'node:child_process'
import { existsSync, mkdirSync, readdirSync, statSync, writeFileSync, rmSync } from 'node:fs'
import { join, resolve } from 'node:path'

const VERSION = process.env.RECORDING_VERSION ?? '0.1.0'
const frontendRoot = resolve(import.meta.dirname, '..')
const repoRoot = resolve(frontendRoot, '..')
const testResultsDir = join(frontendRoot, 'test-results')
const recordingsDir = join(repoRoot, '.recordings')

function findWebmFiles(dir) {
  const out = []
  for (const entry of readdirSync(dir, { withFileTypes: true })) {
    const full = join(dir, entry.name)
    if (entry.isDirectory()) out.push(...findWebmFiles(full))
    else if (entry.name.endsWith('.webm')) out.push(full)
  }
  return out
}

if (!existsSync(testResultsDir)) {
  console.error(`No ${testResultsDir} found — run "npm run e2e" first.`)
  process.exit(1)
}

const videos = findWebmFiles(testResultsDir).sort(
  (a, b) => statSync(a).mtimeMs - statSync(b).mtimeMs,
)

if (videos.length === 0) {
  console.error('No .webm recordings found under test-results/.')
  process.exit(1)
}

mkdirSync(recordingsDir, { recursive: true })
const date = new Date().toISOString().slice(0, 10)
const outFile = join(recordingsDir, `v${VERSION}-${date}.webm`)

const listFile = join(testResultsDir, 'concat-list.txt')
writeFileSync(listFile, videos.map((v) => `file '${v.replace(/'/g, "'\\''")}'`).join('\n'))

execFileSync('ffmpeg', ['-y', '-f', 'concat', '-safe', '0', '-i', listFile, '-c', 'copy', outFile])
rmSync(listFile)

console.log(`Wrote ${videos.length} clip(s) -> ${outFile}`)
