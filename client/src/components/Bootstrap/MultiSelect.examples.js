import React from 'react'
// import f from 'lodash'

import { Row, Col, StatefulForm } from '.'
import MultiSelect, { MultiSelectPlain } from './MultiSelect'

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
        { value: '2-2', label: 'Option 2.2', disabled: true },
        { value: '2-3', label: 'Option 2.3', disabled: true }
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
    name: 'MultiSelectPlain',
    content: (
      <StatefulForm
        idPrefix="example-form-1"
        values={{
          select1: exampleMultipleOptgroups.initialSelected
        }}
      >
        {({ fields, formPropsFor }) => (
          <Row>
            <Col sm>
              <MultiSelectPlain
                {...formPropsFor('select1')}
                options={exampleMultipleOptgroups.multiSelectOptions}
              />
            </Col>
            <Col sm>
              <pre className="my-3">
                <mark>
                  {JSON.stringify(
                    { formProps: formPropsFor('select1'), fields },
                    0,
                    2
                  )}
                </mark>
              </pre>
            </Col>
          </Row>
        )}
      </StatefulForm>
    )
  },
  {
    name: 'MultiSelect',
    content: (
      <StatefulForm
        idPrefix="example-form-2"
        values={{
          select2: exampleMultipleOptgroups.initialSelected
        }}
      >
        {({ fields, formPropsFor }) => (
          <Row>
            <Col sm>
              <MultiSelect
                {...formPropsFor('select2')}
                value={fields.select2 || []}
                options={exampleMultipleOptgroups.multiSelectOptions}
              />
            </Col>
            <Col sm>
              <pre className="my-3">
                <mark>{JSON.stringify({ fields }, 0, 2)}</mark>
              </pre>
            </Col>
          </Row>
        )}
      </StatefulForm>
    )
  }
]
