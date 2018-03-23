import React from 'react'
import f from 'lodash'

// pick and choose what we use, then export all in one object w/ defaults
// import Checkmark from 'react-icons/lib/fa/check'

// const ICONS = {
//   Checkmark: {
//     src: Checkmark,
//     description: 'Save Buttons and other kinds of confirmations'
//     // },
//     // Exchange: {
//     //   component: Exchange
//   }
// }

// const Icons = f.fromPairs(
//   f.map(ICONS, (v, k) => {
//     const LibIcon = ICONS[k].src
//     return [
//       k,
//       function Icon(props) {
//         if (props.children) {
//           throw new Error('Icons cant have `children`!')
//         }
//         return <LibIcon style={{ verticalAlign: 'text-bottom' }} {...props} />
//       }
//     ]
//   })
// )

// if only FA is used, this is enough:
import FontAwesomeIcon from '@fortawesome/react-fontawesome'
import faCheck from '@fortawesome/fontawesome-free-solid/faCheck'
import faExchange from '@fortawesome/fontawesome-free-solid/faExchangeAlt'
import faCalendarAlt from '@fortawesome/fontawesome-free-solid/faCalendarAlt'
import faTrashAlt from '@fortawesome/fontawesome-free-solid/faTrashAlt'
import faChartPie from '@fortawesome/fontawesome-free-solid/faChartPie'

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
  ProcurementLogo: { src: faChartPie }
}

const Icons = f.fromPairs(
  f.map(ICONS, (v, k) => {
    const faIcon = ICONS[k].src
    return [
      k,
      function Icon(props) {
        if (props.children) {
          throw new Error('Icons cant have `children`!')
        }
        return <FontAwesomeIcon icon={faIcon} {...props} />
      }
    ]
  })
)

export default Icons
