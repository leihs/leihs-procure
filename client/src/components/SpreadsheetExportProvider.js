import React from 'react'
import f from 'lodash'
import XLSX from 'xlsx'
import t from '../locale/translate'
import {
  DisplayName,
  RequestTotalAmount,
  RequestFieldValue
} from '../components/decorators'

const EXPORT_FORMATS = [
  { type: 'xlsx', ext: 'xlsx', name: '.xlsx (Excel 2007+)' },
  { type: 'biff8', ext: 'xls', name: '.xls (Excel 97-2004)' },
  { type: 'ods', ext: 'ods', name: '.ods (LibreOffice, …)' },
  { type: 'csv', ext: 'csv', name: '.csv' }
]

class SpreadsheetExporter extends React.Component {
  render({ requestsData, children } = this.props) {
    const table = convertToTablularData(requestsData)
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
  const rows = [table.headers, ...table.rows]
  const workSheet = XLSX.utils.aoa_to_sheet(rows)
  const workBook = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(workBook, workSheet, title || t('app_title'))
  XLSX.writeFile(workBook, `procure_${calendarDate}.${ext}`)
}

// NOTE: conversion of dashboard data here
const REQ_NUM_PREFIX = '#' // prevent Excel from auto-converting this field (into a Date or Floating Point Number depending on locale)
const spreadSheetCols = [
  {
    key: 'short_id',
    label: 'Nummer',
    fn: r => REQ_NUM_PREFIX + f.get(r, 'short_id')
  },
  {
    key: 'budget_period',
    label: 'Budgetperiode',
    fn: r => f.get(r, 'budget_period.value.name')
  },
  {
    key: 'main_category',
    label: 'Hauptkategorie',
    fn: r => f.get(r, 'category.value.main_category.name')
  },
  {
    key: 'category',
    label: 'Subkategorie',
    fn: r => f.get(r, 'category.value.name')
  },
  { key: 'user', label: 'Antragsteller/in' },
  {
    key: 'department',
    label: 'Departement',
    fn: r => f.get(r, 'organization.value.department.name')
  },
  {
    key: 'organization',
    label: 'Organisation',
    fn: r => f.get(r, 'organization.value.name')
  },
  { key: 'article_name', label: 'Artikel oder Projekt' },
  {
    key: 'article_number',
    label: 'Artikelnr. oder Herstellernr.'
  },
  {
    key: 'supplier',
    label: 'Lieferant',
    fn: r => f.get(r, 'supplier.value.id') || f.get(r, 'supplier_name.value')
  },
  { key: 'requested_quantity', label: 'Menge beantragt' },
  { key: 'approved_quantity', label: 'Menge bewilligt' },
  { key: 'order_quantity', label: 'Bestellmenge' },
  {
    key: 'price',
    label: 'Preis inkl. MwSt',
    fn: r => (f.get(r, 'price_cents.value') || 0) / 100
  },
  {
    key: 'total_price',
    label: 'Total inkl. MwSt',
    fn: r => f.try(_ => (RequestTotalAmount(r) || 0) / 100)
  },
  {
    key: 'state',
    label: 'Status',
    fn: r => RequestFieldValue('state', r)
  },
  {
    key: 'priority',
    label: 'Priorität',
    fn: r => f.try(_ => RequestFieldValue('priority', r))
  },
  {
    key: 'inspector_priority',
    label: 'Priorität des Prüfers',

    fn: r => f.try(_ => RequestFieldValue('inspector_priority', r))
  },
  {
    key: 'Ersatz / Neu',
    label: 'Ersatz / Neu',
    fn: r => f.try(_ => RequestFieldValue('replacement', r))
  },
  { key: 'receiver', label: 'Receiver' },
  {
    key: 'building',
    label: 'Gebäude',
    fn: r => f.get(r, 'room.value.building.name')
  },
  { key: 'room', label: 'Raum', fn: r => f.get(r, 'room.value.name') },
  { key: 'motivation', label: 'Begründung' },
  { key: 'inspection_comment', label: 'Kommentar des Prüfers' },
  {
    key: 'accounting_type',
    label: 'Abrechnungsart',
    fn: r => RequestFieldValue('accounting_type', r)
  },
  { key: 'cost_center', label: 'Kostenstelle' },
  { key: 'general_ledger_account', label: 'Sachkonto' },
  { key: 'procurement_account', label: 'Beschaffungskonto' },
  { key: 'internal_order_number', label: 'Innenauftrag' },
  {
    key: 'url',
    label: 'Link',
    fn: r => `https://leihs.example.com/procure/requests/${r.id}`
  }
]

function convertToTablularData(data) {
  const table = f.flatMap(f.get(data, 'dashboard.budget_periods'), bp =>
    f.flatMap(bp.main_categories, mc =>
      f.flatMap(mc.categories, sc =>
        f.map(sc.requests, r =>
          f.fromPairs(
            spreadSheetCols.map(col => [col.key, getFieldValue(col, r)])
          )
        )
      )
    )
  )
  const cols = spreadSheetCols.map(c => ({ key: c.key, name: c.label }))
  const headers = f.map(cols, 'name')
  const rows = f.map(table, row => f.map(cols, ({ key }) => row[key]))

  return { table, headers, cols, rows }
}

function getFieldValue(col, request) {
  if (col.fn) return col.fn(request)
  if (!f.has(request, col.key)) throw new Error(`missing field '${col.key}'!`)
  const val = f.get(request, [col.key, 'value'])
  return (
    ((f.isString(val) || f.isNumber(val)) && val) ||
    f.try(() => DisplayName(val)) ||
    ''
  )
}
