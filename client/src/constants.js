import f from 'lodash'

export const REQUEST_PRIORITIES = ['NORMAL', 'HIGH']

export const REQUEST_INSPECTOR_PRIORITIES = [
  'LOW',
  'MEDIUM',
  'HIGH',
  'MANDATORY'
]

export const REQUEST_REPLACEMENT_VALUES_MAP = {
  true: 'replacement',
  false: 'new'
}
export const REQUEST_REPLACEMENT_VALUES = f.values(
  REQUEST_REPLACEMENT_VALUES_MAP
)

export const REQUEST_STATES = [
  'NEW',
  'IN_APPROVAL',
  'APPROVED',
  'PARTIALLY_APPROVED',
  'DENIED'
]
