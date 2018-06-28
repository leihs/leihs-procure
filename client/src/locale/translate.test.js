import t from './translate'

it('translates simple key', () => {
  expect(t('form_btn_save')).toEqual('Speichern')
})

it('translates key with context', () => {
  expect(t('request_form_field.article_name')).toEqual('Artikel oder Projekt')
})

it('translates key with context and falls back', () => {
  expect(
    t('this_context_does_not_exist.request_form_field.article_name')
  ).toEqual('Artikel oder Projekt')
})
