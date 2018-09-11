import React from 'react'
import f from 'lodash'
import XLSX from 'xlsx'
import t from '../locale/translate'
import { DisplayName } from '../components/decorators'
import { SPREADSHEET_EXPORT_COLS } from '../constants'

const EXPORT_FORMATS = [
  { type: 'xlsx', ext: 'xlsx', name: '.xlsx (Excel 2007+)' },
  { type: 'biff8', ext: 'xls', name: '.xls (Excel 97-2004)' },
  { type: 'ods', ext: 'ods', name: '.ods (LibreOffice, …)' },
  { type: 'csv', ext: 'csv', name: '.csv' }
]

const withCols = SPREADSHEET_EXPORT_COLS.map(key => ({
  key,
  name: t(`request_form_field.${key}`)
}))

class SpreadsheetExporter extends React.Component {
  render({ requestsData, children } = this.props) {
    const table = convertToTablularData(requestsData, withCols)
    return children({
      table,
      download: exportAndDownloadSpreadsheet,
      exportFormats: EXPORT_FORMATS
    })
  }
}

export default SpreadsheetExporter

function exportAndDownloadSpreadsheet(table, { type, ext }, title) {
  const calendarDate = new Date().toISOString().split('T')[0]
  const rows = type === 'csv' ? [table.headers, ...table.rows] : table.rows
  const workSheet = XLSX.utils.aoa_to_sheet(rows)
  const workBook = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(workBook, workSheet, title || t('app_title'))
  XLSX.writeFile(workBook, `procure_${calendarDate}.${ext}`)
}

// TMP: conversion of dashboard data

function convertToTablularData(requests, cols) {
  const table = f.flatMap(f.get(requests, 'dashboard.budget_periods'), bp =>
    f.flatMap(bp.main_categories, mc =>
      f.flatMap(mc.categories, sc =>
        f.map(sc.requests, r =>
          f.fromPairs(f.map(r, (field, key) => [key, getFieldValue(field)]))
        )
      )
    )
  )
  const headers = f.keys(f.first(table)).filter(key => f.find(cols, { key }))
  const rows = f.map(table, row => f.map(headers, key => row[key]))

  return { table, headers, cols, rows }
}

function getFieldValue(field) {
  return (
    f.try(() => DisplayName(field.value)) ||
    stringIfHas(field, 'value.name') ||
    stringIfHas(field, 'value.id') ||
    stringIfHas(field, 'value') ||
    stringIfHas(field)
  )
}

function stringIfHas(obj, path, fn = JSON.stringify) {
  const val = !path ? obj : f.get(obj, path)
  return f.isUndefined(val) || f.isString(val)
    ? val
    : f.try(() => fn(val)) || String(val)
}
