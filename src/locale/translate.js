import f from 'lodash'

const translate = key => {
  const fallback = `⟪${key}⟫`
  // 'foo.bar.baz' => [ 'foo.bar.baz', 'bar.baz', 'baz' ]
  const paths = key.split('.').map((i, n, a) => a.slice(n).join('.'))
  const results = paths
    .map(k => f.get(translations, k))
    .filter(r => f.isString(r) || f.isNumber(r))
  return f.first(results) || fallback
}
export default translate

const translations = {
  form_btn_save: 'Speichern',
  form_btn_cancel: 'Abbrechen',
  form_btn_move_category: 'Kategorie ändern',
  form_btn_change_budget_period: 'Budgetperiode ändern',
  form_btn_delete: 'Löschen',
  form_filepicker_label: 'Datei auswählen',
  request_state: 'Status',

  request_priority: 'Priorität',
  request_priority_label_normal: 'Normal',
  request_priority_label_high: 'Hoch',

  request_priority_inspector: 'Priorität des Prüfers',
  request_priority_inspector_label_low: 'Tief',
  request_priority_inspector_label_medium: 'Mittel',
  request_priority_inspector_label_high: 'Hoch',
  request_priority_inspector_label_mandatory: 'Zwingend',

  request_form_field: {
    article_name: 'Artikel oder Projekt',
    article_number: 'Artikelnr. oder Herstellernr.',
    supplier: 'Lieferant',
    receiver: 'Name des Empfängers',
    building: 'Gebäude',
    room: 'Raum',
    motivation: 'Begründung',

    replacement: 'Ersatz / Neu',
    request_replacement_labels_new: 'Neu',
    request_replacement_labels_replacement: 'Ersatz',
    price: 'Stückpreis CHF',
    price_help: 'inkl. MwSt',
    quantity_requested: 'Menge beantragt',
    quantity_approved: 'Menge bewilligt',
    quantity_ordered: 'Bestellmenge',
    price_total: 'Total CHF',
    price_total_help: 'inkl. MwSt',
    inspection_comment: 'Kommentar des Prüfers',
    attachments: 'Anhänge',
    accounting_type: 'Abrechnungsart',
    accounting_type_label_investment: 'Investition',
    accounting_type_label_aquisition: 'Beschaffung',
    cost_center: 'Kostenstelle',
    general_ledger_account: 'Sachkonto',
    internal_order_number: 'Innenauftrag'
  },
  accounting_type_investment: 'Investition',
  accounting_type_aquisition: 'Beschaffung'
}
