import React, { Fragment as F } from 'react'
import cx from 'classnames'
import PropTypes from 'prop-types'
import f from 'lodash'
import ButtonDropdown from 'reactstrap/lib/ButtonDropdown'
import DropdownToggle from 'reactstrap/lib/DropdownToggle'
import DropdownMenu from 'reactstrap/lib/DropdownMenu'
import DropdownItem from 'reactstrap/lib/DropdownItem'
import logger from 'debug'
const log = logger('app:ui:MultiSelect')

const START_OPEN = true
const txt_select_all = 'Alle auswählen'
const txt_all_selected = 'Alle ausgew\xE4hlt'
const txt_none_selected = 'Keine ausgew\xE4hlt'
const txt_n_selected = ` selektiert`

// styles
const menuCls = 'py-2 px-0'
const itemCls = 'py-1 px-2 f6'
const groupHeaderCls = 'py-1 px-2 f6 font-weight-bold'
const groupWrapCls = 'dropdown-item-subgroup'
const groupItemCls = [itemCls, 'pl-4']

class MultiSelect extends React.PureComponent {
  constructor(props) {
    super(props)
    this.state = { dropdownOpen: START_OPEN }
    log('init', { props, state: this.state })
    this.onSelectAllChange = this.onSelectAllChange.bind(this)
  }

  allOptions = () => f.map(this.props.options, 'options')
  allSelected = () => this.allOptions().length === this.props.value.length

  toggleDropdown = () => {
    this.setState(cur => ({ dropdownOpen: !cur.dropdownOpen }))
  }

  onCheckboxChange = (event, value) => {
    log('onCheckboxChange', { value, event })
    const isSelected = event.target.checked
    const item = value || event.target.name
    const selected = this.props.value
      .concat(item)
      .filter(val => (val === item ? isSelected : true))
    this.onChange(selected)
  }

  onSelectAllChange = ({ target }) => {
    log('onSelectAllChange', { target: target })

    // eslint-disable-next-line no-debugger
    debugger

    const isSelected = target.checked
    const values = !isSelected ? [] : f.map(this.allOptions(), 'value')
    this.onChange(values)
  }

  onChange = value => {
    const name = this.props.name
    log('onChange', { name, value })
    this.props.onChange({ target: { name, value } })
  }

  render({ props, state } = this) {
    const { options, name, ...restProps } = props
    const selected = props.value
    const allOptions = this.allOptions()
    const allSelected = this.allSelected()
    const Id = s => `${restProps.id}_${s}`

    return (
      <F>
        <ButtonDropdown
          {...restProps}
          isOpen={this.state.dropdownOpen}
          toggle={this.toggleDropdown}
        >
          <DropdownToggle caret outline>
            {titleBySelection(selected, allOptions)}
          </DropdownToggle>
          <DropdownMenu className={cx(menuCls)}>
            <DropdownItem
              toggle={false}
              className={cx(itemCls, { active: allSelected })}
            >
              <Checkbox
                id={Id('select_all')}
                label={txt_select_all}
                checked={allSelected}
                onChange={this.onSelectAllChange}
              />
            </DropdownItem>
            <DropdownItem divider />

            {options.map(({ label, options }, i) => (
              <F key={i}>
                <DropdownItem toggle={false} className={cx(groupHeaderCls)}>
                  <Checkbox
                    id={Id('select_all')}
                    label={label}
                    checked={allSelected}
                    onChange={this.onSelectAllChange}
                  />
                </DropdownItem>
                <div className={cx(groupWrapCls)}>
                  {options.map(({ label, value, ...props }, ii) => {
                    const isSelected = selected.indexOf(value) !== -1
                    return (
                      <DropdownItem
                        key={ii}
                        toggle={false}
                        className={cx(groupItemCls, { active: isSelected })}
                      >
                        <Checkbox
                          {...props}
                          id={Id(`${value}-checkbox`)}
                          label={label}
                          value={value}
                          checked={isSelected}
                          onChange={e => this.onCheckboxChange(e, value)}
                        />
                      </DropdownItem>
                    )
                  })}
                </div>
              </F>
            ))}
          </DropdownMenu>
        </ButtonDropdown>
        <pre className="my-3">
          <mark>{JSON.stringify({ props: this.props }, 0, 2)}</mark>
        </pre>
      </F>
    )
  }
}

MultiSelect.defaultProps = {
  name: '',
  options: [],
  value: [],
  onChange: f.noop
}

MultiSelect.propTypes = {
  name: PropTypes.string.isRequired,
  value: PropTypes.arrayOf(PropTypes.string.isRequired),
  options: PropTypes.arrayOf(
    PropTypes.shape({
      label: PropTypes.node.isRequired,
      options: PropTypes.arrayOf(
        PropTypes.shape({
          value: PropTypes.string.isRequired,
          label: PropTypes.node.isRequired
        })
      )
    })
  ).isRequired
}

export default MultiSelect

const Checkbox = ({ id, className, label, ...props }) => (
  <div className="custom-control custom-checkbox">
    <input
      type="checkbox"
      className={cx(className, 'custom-control-input')}
      id={id}
      {...props}
    />
    <label className="custom-control-label" htmlFor={id}>
      {label}
    </label>
  </div>
)

function titleBySelection(selected, allOptions) {
  const count = selected.length
  const optionsCount = allOptions.length
  if (count === optionsCount) {
    return txt_all_selected
  }
  if (count === 0) {
    return txt_none_selected
  }
  if (count > 3) {
    return `${count}${txt_n_selected}`
  }
  return selected
    .map(item => allOptions.filter(({ value }) => value === item)[0])
    .map(item => item && item.label)
    .join(', ')
}
