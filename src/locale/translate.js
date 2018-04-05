import f from 'lodash'

const translate = key => {
  return f.get(translations, key) || `⟪${key}⟫`
}

const translations = {
  form_btn_save: 'Speichern',
  form_btn_cancel: 'Abbrechen',
  form_btn_move_category: 'Kategorie ändern',
  form_btn_change_budget_period: 'Budgetperiode ändern',
  form_btn_delete: 'Löschen',
  form_filepicker_label: 'Datei auswählen',
  request_state: 'Status',
  field: {
    article: 'Artikel oder Projekt',
    article_nr: 'Artikelnr. oder Herstellernr.',
    supplier: 'Lieferant',
    receiver_name: 'Name des Empfängers',
    building: 'Gebäude',
    room: 'Raum',
    purpose: 'Begründung',
    priority_requester: 'Priorität',
    request_priority_requester_labels: ['Tief', 'Mittel', 'Hoch', 'Zwingend'],
    priority_inspector: 'Priorität des Prüfers',
    request_priority_inspector_labels: ['Tief', 'Mittel', 'Hoch', 'Zwingend'],
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
    comment_inspector: 'Kommentar des Prüfers',
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

export default translate
