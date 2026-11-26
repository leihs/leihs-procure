import { utils, write } from 'xlsx'
import { test, expect } from 'vitest'

/**
 * Smoke: @e965/xlsx@0.20.3 still supports the SpreadsheetExportProvider call site
 * (aoa_to_sheet + book_new/append + write for xlsx/xls/ods/csv).
 */
const EXPORT_FORMATS = [
  { type: 'xlsx', bookType: 'xlsx' },
  { type: 'biff8', bookType: 'biff8' },
  { type: 'ods', bookType: 'ods' },
  { type: 'csv', bookType: 'csv' }
]

function buildSampleWorkbook() {
  const rows = [
    ['Nummer', 'Artikel oder Projekt', 'Preis inkl. MwSt'],
    ['#42', 'Testkamera', 12.5]
  ]
  const workSheet = utils.aoa_to_sheet(rows)
  const workBook = utils.book_new()
  utils.book_append_sheet(workBook, workSheet, 'Leihs')
  return workBook
}

function bufferToUtf8(buf) {
  const bytes = buf instanceof ArrayBuffer ? new Uint8Array(buf) : buf
  return new TextDecoder('utf-8').decode(bytes)
}

test.each(EXPORT_FORMATS)(
  'writes non-empty $type workbook buffer',
  ({ bookType }) => {
    const workBook = buildSampleWorkbook()
    const buf = write(workBook, { bookType, type: 'buffer' })
    expect(buf).toBeTruthy()
    expect(buf.byteLength ?? buf.length).toBeGreaterThan(20)
  }
)

test('csv export contains header and sample values', () => {
  const workBook = buildSampleWorkbook()
  const buf = write(workBook, { bookType: 'csv', type: 'buffer' })
  const text = bufferToUtf8(buf)
  expect(text).toContain('Nummer')
  expect(text).toContain('Testkamera')
  expect(text).toContain('#42')
})

test('xlsx workbook sheet round-trips sample rows', () => {
  const workBook = buildSampleWorkbook()
  write(workBook, { bookType: 'xlsx', type: 'buffer' })
  const roundTrip = utils.sheet_to_json(workBook.Sheets['Leihs'], {
    header: 1
  })
  expect(roundTrip[0][0]).toBe('Nummer')
  expect(roundTrip[1][1]).toBe('Testkamera')
})
