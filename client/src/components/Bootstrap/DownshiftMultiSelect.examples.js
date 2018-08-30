import React, { Fragment as F } from 'react'
// import f from 'lodash'

import { StatefulForm } from '.'
import MultiSelect from './DownshiftMultiSelect'

// # DATA

const exampleMultipleOptgroups = {
  initialSelected: ['1-2', '1-3'],
  multiSelectOptions: [
    {
      label: 'Group 1',
      options: [
        { value: '1-1', label: 'Option 1.1' },
        { value: '1-2', label: 'Option 1.2' },
        { value: '1-3', label: 'Option 1.3' }
      ]
    },
    {
      label: 'Group 2',
      options: [
        { value: '2-1', label: 'Option 2.1' },
        { value: '2-2', label: 'Option 2.2' },
        { value: '2-3', label: 'Option 2.3' }
      ]
    },
    {
      label: 'Group 3',
      options: [
        { value: '3-1', label: 'Option 3.1', disabled: true },
        { value: '3-2', label: 'Option 3.2' },
        { value: '3-3', label: 'Option 3.3' }
      ]
    }
  ]
}

export const examples = [
  {
    name: 'MultiSelect',
    content: (
      <div>
        <StatefulForm
          idPrefix="example-form-1"
          values={{
            exampleMultiselect: exampleMultipleOptgroups.initialSelected
          }}
        >
          {({ fields, formPropsFor }) => (
            <F>
              <MultiSelect
                {...formPropsFor('exampleMultiselect')}
                id="example-multiple-optgroups"
                options={exampleMultipleOptgroups.multiSelectOptions}
              />
              <pre className="my-3">
                <mark>{JSON.stringify({ fields }, 0, 2)}</mark>
              </pre>
            </F>
          )}
        </StatefulForm>
      </div>
    )
  }
]
