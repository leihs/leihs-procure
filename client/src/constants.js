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

export const REQUEST_STATES_MAP = [
  {
    key: 'NEW',
    bsColor: 'info',
    roles: ['isRequester', 'isInspector', 'isViewer', 'isAdmin']
  },
  {
    key: 'IN_APPROVAL',
    bsColor: 'primary',
    roles: ['isOnlyRequester']
  },
  {
    key: 'APPROVED',
    bsColor: 'success',
    roles: ['isRequester', 'isInspector', 'isViewer', 'isAdmin']
  },
  {
    key: 'PARTIALLY_APPROVED',
    bsColor: 'warning',
    roles: ['isRequester', 'isInspector', 'isViewer', 'isAdmin']
  },
  {
    key: 'DENIED',
    bsColor: 'danger',
    roles: ['isRequester', 'isInspector', 'isViewer', 'isAdmin']
  }
]

export const REQUEST_STATE_COLORS = f.fromPairs(
  f.map(REQUEST_STATES_MAP, i => [i.key, i.bsColor])
)
export const REQUEST_STATES = f.map(REQUEST_STATES_MAP, 'key')

export const ORDER_STATI_MAP = [
  {
    key: 'NOT_PROCESSED',
    bsColor: 'info',
    roles: ['isRequester', 'isInspector', 'isViewer', 'isAdmin']
  },
  {
    key: 'IN_PROGRESS',
    bsColor: 'info',
    roles: ['isRequester', 'isInspector', 'isViewer', 'isAdmin']
  },
  {
    key: 'PROCURED',
    bsColor: 'success',
    roles: ['isRequester', 'isInspector', 'isViewer', 'isAdmin']
  },
  {
    key: 'ALTERNATIVE_PROCURED',
    bsColor: 'success',
    roles: ['isRequester', 'isInspector', 'isViewer', 'isAdmin']
  },
  {
    key: 'NOT_PROCURED',
    bsColor: 'danger',
    roles: ['isRequester', 'isInspector', 'isViewer', 'isAdmin']
  }
]

export const ORDER_STATUS_COLORS = f.fromPairs(
  f.map(ORDER_STATI_MAP, i => [i.key, i.bsColor])
)
export const ORDER_STATI = f.map(ORDER_STATI_MAP, 'key')
