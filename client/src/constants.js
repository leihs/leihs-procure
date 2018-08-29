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

const requestStatesMap = [
  { key: 'NEW', bsColor: 'info' },
  { key: 'IN_APPROVAL', bsColor: 'primary' },
  { key: 'APPROVED', bsColor: 'success' },
  { key: 'PARTIALLY_APPROVED', bsColor: 'warning' },
  { key: 'DENIED', bsColor: 'danger' }
]

export const REQUEST_STATE_COLORS = f.fromPairs(
  f.map(requestStatesMap, i => [i.key, i.bsColor])
)
export const REQUEST_STATES = f.map(requestStatesMap, 'key')
