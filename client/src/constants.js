import f from 'lodash'

export const REQUEST_PRIORITIES = ['NORMAL', 'HIGH']

export const REQUEST_INSPECTOR_PRIORITIES = [
  'LOW',
  'MEDIUM',
  'HIGH',
  'MANDATORY'
]

// TODO: ENUMs
export const REQUEST_REPLACEMENT_VALUES_MAP = {
  true: 'replacement',
  false: 'new'
}
export const REQUEST_REPLACEMENT_VALUES = f.values(
  REQUEST_REPLACEMENT_VALUES_MAP
)

// FIXME: add 'IN_APPROVAL'
export const REQUEST_STATES = [
  'NEW',
  'APPROVED',
  'PARTIALLY_APPROVED',
  'DENIED'
]
