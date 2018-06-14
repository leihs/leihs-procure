import React from 'react'

import MultiSelect from './DownshiftMultiSelect'

// # DATA

const exampleMultipleOptgroups = [
  {
    label: 'Group 1',
    options: [
      { value: '1-1', label: 'Option 1.1' },
      { value: '1-2', label: 'Option 1.2', selected: true },
      { value: '1-3', label: 'Option 1.3', selected: true }
    ]
  },
  {
    label: 'Group 2',
    options: [
      { value: '2-1', label: 'Option 2.1' },
      { value: '2-2', label: 'Option 2.2' },
      { value: '2-3', label: 'Option 2.3' }
    ]
  }
]

export const examples = [
  {
    name: 'MultiSelect',
    content: (
      <div>
        <MultiSelect
          id="example-multiple-optgroups"
          options={exampleMultipleOptgroups}
        />
      </div>
    )
  }
]
