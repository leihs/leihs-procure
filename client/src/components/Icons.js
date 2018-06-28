import React from 'react'
import f from 'lodash'
import cx from 'classnames'

import FontAwesomeIcon from '@fortawesome/react-fontawesome'
// pick & choose what we use, name & describe it, then export all in one object of components
import faCheck from '@fortawesome/fontawesome-free-solid/faCheck'
import faExchange from '@fortawesome/fontawesome-free-solid/faExchangeAlt'
import faCalendar from '@fortawesome/fontawesome-free-solid/faCalendar'
import faCalendarCheck from '@fortawesome/fontawesome-free-solid/faCalendarCheck'
import faCalendarAlt from '@fortawesome/fontawesome-free-solid/faCalendarAlt'
import faCalendarPlus from '@fortawesome/fontawesome-free-solid/faCalendarPlus'
import faTrashAlt from '@fortawesome/fontawesome-free-solid/faTrashAlt'
import faChartPie from '@fortawesome/fontawesome-free-solid/faChartPie'
import faPaperclip from '@fortawesome/fontawesome-free-solid/faPaperclip'
import faCircle from '@fortawesome/fontawesome-free-solid/faCircle'
import faCheckCircle from '@fortawesome/fontawesome-free-solid/faCheckCircle'
import faCircleNotch from '@fortawesome/fontawesome-free-solid/faCircleNotch'
import faTag from '@fortawesome/fontawesome-free-solid/faTag'
import faQuestion from '@fortawesome/fontawesome-free-solid/faQuestion'
import faShoppingCart from '@fortawesome/fontawesome-free-solid/faShoppingCart'
import faUserCircle from '@fortawesome/fontawesome-free-solid/faUserCircle'
import faTimes from '@fortawesome/fontawesome-free-solid/faTimes'
import faCaretUp from '@fortawesome/fontawesome-free-solid/faCaretUp'
import faCaretDown from '@fortawesome/fontawesome-free-solid/faCaretDown'
import faCaretLeft from '@fortawesome/fontawesome-free-solid/faCaretLeft'
import faCaretRight from '@fortawesome/fontawesome-free-solid/faCaretRight'
import faSyncAlt from '@fortawesome/fontawesome-free-solid/faSyncAlt'

const ICONS = {
  CaretUp: {
    src: faCaretUp,
    extraProps: { fixedWidth: true }
  },
  CaretDown: {
    src: faCaretDown,
    extraProps: { fixedWidth: true }
  },
  CaretLeft: {
    src: faCaretLeft,
    extraProps: { fixedWidth: true }
  },
  CaretRight: {
    src: faCaretRight,
    extraProps: { fixedWidth: true }
  },
  Checkmark: {
    src: faCheck,
    description: 'Save Buttons and other kinds of confirmations'
  },
  Cross: {
    src: faTimes
  },
  Exchange: {
    src: faExchange
  },
  RequestingPhase: {
    src: faCalendarPlus,
    extraProps: { transform: 'up-1' }
  },
  InspectionPhase: {
    src: faCalendarCheck,
    extraProps: { transform: 'up-1' }
  },
  BudgetPeriod: {
    src: faCalendar,
    extraProps: { transform: 'up-1' }
  },
  Calendar: {
    src: faCalendarAlt
  },
  Trash: {
    src: faTrashAlt,
    description: 'Deleting things'
  },
  ProcurementLogo: { src: faChartPie },
  Paperclip: {
    src: faPaperclip
  },
  RadioCheckedOn: {
    src: faCheckCircle
  },
  RadioCheckedOff: {
    src: faCircle
  },
  Spinner: {
    src: faCircleNotch,
    extraProps: { className: 'fa-spin' }
  },
  PriceTag: {
    src: faTag,
    extraProps: { flip: 'horizontal' }
  },
  QuestionMark: {
    src: faQuestion
  },
  ShoppingCart: {
    src: faShoppingCart
  },
  User: {
    src: faUserCircle
  },
  Reload: {
    src: faSyncAlt
  }
}

const Icons = f.fromPairs(
  f.map(ICONS, ({ src, extraProps = {} }, name) => {
    const iconComponent = ({ spaced, ...givenProps }) => {
      if (givenProps.children) {
        throw new Error('Icons cant have `children`!')
      }
      const iconClassName = cx(extraProps.className, givenProps.className, {
        'mr-1': spaced
      })
      return (
        <FontAwesomeIcon
          {...extraProps}
          {...givenProps}
          icon={src}
          className={iconClassName}
        />
      )
    }
    iconComponent.displayName = `Icon.${name}`
    return [name, iconComponent]
  })
)

export default Icons
