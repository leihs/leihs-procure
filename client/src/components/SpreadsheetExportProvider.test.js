import React from 'react'
import { utils, write, read } from 'xlsx'
import { test, expect, vi, beforeEach, afterEach, describe } from 'vitest'
import TestRenderer, { act } from 'react-test-renderer'

vi.mock('xlsx', async (importOriginal) => {
  const actual = await importOriginal()
  return {
    ...actual,
    writeFile: vi.fn()
  }
})

vi.mock('lodash', async (importOriginal) => {
  const actual = await importOriginal()
  const f = actual.default
  if (!f.try) {
    f.try = (fn) => {
      try {
        return fn()
      } catch {
        return undefined
      }
    }
  }
  return { ...actual, default: f }
})

vi.mock('../locale/translate', () => ({
  default: (key) => key
}))

vi.mock('../components/decorators', () => ({
  DisplayName: (val) => (val && val.name) || '',
  RequestTotalAmount: () => 1250,
  RequestFieldValue: () => 'x'
}))

import { writeFile } from 'xlsx'
import SpreadsheetExporter from './SpreadsheetExportProvider.jsx'

/**
 * Smoke: npm `xlsx` (^0.18.5) APIs used by SpreadsheetExportProvider
 * (aoa_to_sheet + book_new/append + write), including write→read round-trip.
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

test('xlsx workbook sheet round-trips sample rows via write+read', () => {
  const workBook = buildSampleWorkbook()
  const buf = write(workBook, { bookType: 'xlsx', type: 'buffer' })
  const reopened = read(buf, { type: 'buffer' })
  const roundTrip = utils.sheet_to_json(reopened.Sheets['Leihs'], {
    header: 1
  })
  expect(roundTrip[0][0]).toBe('Nummer')
  expect(roundTrip[1][1]).toBe('Testkamera')
})

function field(value) {
  return { value }
}

function sampleRequestsData() {
  return {
    dashboard: {
      budget_periods: [
        {
          main_categories: [
            {
              categories: [
                {
                  requests: [
                    {
                      id: 'req-1',
                      short_id: '42',
                      budget_period: field({ name: '2026' }),
                      category: field({
                        name: 'Cameras',
                        main_category: { name: 'AV' }
                      }),
                      user: field('Ada'),
                      organization: field({
                        name: 'Org',
                        department: { name: 'Dep' }
                      }),
                      article_name: field('Testkamera'),
                      article_number: field('SKU-1'),
                      supplier: field({ id: 'sup-1' }),
                      supplier_name: field(''),
                      requested_quantity: field(1),
                      approved_quantity: field(1),
                      order_quantity: field(1),
                      price_cents: field(1250),
                      state: field('new'),
                      priority: field('high'),
                      inspector_priority: field('medium'),
                      replacement: field(false),
                      receiver: field('Ada'),
                      room: field({ name: 'R1', building: { name: 'B1' } }),
                      motivation: field('need'),
                      inspection_comment: field(''),
                      accounting_type: field('a'),
                      cost_center: field('cc'),
                      general_ledger_account: field('gla'),
                      procurement_account: field('pa'),
                      internal_order_number: field('io'),
                      order_status: field('open'),
                      order_comment: field('')
                    }
                  ]
                }
              ]
            }
          ]
        }
      ]
    }
  }
}

describe('SpreadsheetExporter', () => {
  beforeEach(() => {
    writeFile.mockClear()
  })

  afterEach(() => {
    writeFile.mockClear()
  })

  test('download calls writeFile with workbook and .xlsx filename', () => {
    let api
    act(() => {
      TestRenderer.create(
        React.createElement(
          SpreadsheetExporter,
          { requestsData: sampleRequestsData() },
          (props) => {
            api = props
            return null
          }
        )
      )
    })

    expect(api.exportFormats.map((f) => f.type)).toEqual([
      'xlsx',
      'biff8',
      'ods',
      'csv'
    ])
    expect(api.table.headers[0]).toBe('Nummer')
    expect(api.table.rows[0][0]).toBe('#42')

    const xlsxFmt = api.exportFormats.find((f) => f.type === 'xlsx')
    act(() => {
      api.download(api.table, xlsxFmt, 'Leihs')
    })

    expect(writeFile).toHaveBeenCalledTimes(1)
    const [workbook, filename] = writeFile.mock.calls[0]
    expect(filename).toMatch(/^procure_\d{4}-\d{2}-\d{2}\.xlsx$/)
    expect(workbook.SheetNames).toContain('Leihs')
  })
})
