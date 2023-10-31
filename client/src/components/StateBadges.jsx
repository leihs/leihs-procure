import React from 'react'
// import f from 'lodash'
import cx from 'classnames'

import * as CONSTANTS from '../constants'
import t from '../locale/translate'
import { Badge } from './Bootstrap'

const RequestStateBadge = ({ state, className, ...props }) => {
  const cls = cx(`bg-${CONSTANTS.REQUEST_STATE_COLORS[state]}`, className)
  return (
    <Badge dark {...props} className={cls}>
      {t(`request_state_label_${state}`)}
    </Badge>
  )
}

export default RequestStateBadge

export const OrderStatusBadge = ({ state, className, ...props }) => {
  const cls = cx(`bg-${CONSTANTS.ORDER_STATUS_COLORS[state]}`, className)
  return (
    <Badge dark {...props} className={cls}>
      {t(`order_status_label_${state}`)}
    </Badge>
  )
}
