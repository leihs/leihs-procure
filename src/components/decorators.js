import f from 'lodash'

export const RequestTotalAmount = fields => {
  const quantity = f.last(
    f.filter(
      ['requested', 'approved', 'ordered'].map(k => fields[`quantity_${k}`])
    )
  )
  const price = (parseInt(fields.price_cents, 10) || 0) / 100
  return (parseInt(quantity, 10) || 0) * price
}
