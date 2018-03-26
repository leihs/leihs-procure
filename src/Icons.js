import React from 'react'
import f from 'lodash'

// pick & choose what we use, name & describe it, then export all in one object of components
import FontAwesomeIcon from '@fortawesome/react-fontawesome'
import faCheck from '@fortawesome/fontawesome-free-solid/faCheck'
import faExchange from '@fortawesome/fontawesome-free-solid/faExchangeAlt'
import faCalendarAlt from '@fortawesome/fontawesome-free-solid/faCalendarAlt'
import faTrashAlt from '@fortawesome/fontawesome-free-solid/faTrashAlt'
import faChartPie from '@fortawesome/fontawesome-free-solid/faChartPie'
import faPaperclip from '@fortawesome/fontawesome-free-solid/faPaperclip'
import faCircle from '@fortawesome/fontawesome-free-solid/faCircle'
import faCheckCircle from '@fortawesome/fontawesome-free-solid/faCheckCircle'

const ICONS = {
  Checkmark: {
    src: faCheck,
    description: 'Save Buttons and other kinds of confirmations'
  },
  Exchange: {
    src: faExchange
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
  }
}

const Icons = f.fromPairs(
  f.map(ICONS, ({ src }, name) => {
    const iconComponent = props => {
      if (props.children) {
        throw new Error('Icons cant have `children`!')
      }
      return <FontAwesomeIcon icon={src} {...props} />
    }
    iconComponent.displayName = `Icon.${name}`
    return [name, iconComponent]
  })
)

export default Icons
