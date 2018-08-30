import React, { Fragment as F } from 'react'
import cx from 'classnames'
import PropTypes from 'prop-types'
import f from 'lodash'
import Dropdown from 'reactstrap/lib/Dropdown'
import DropdownToggle from 'reactstrap/lib/DropdownToggle'
import DropdownMenu from 'reactstrap/lib/DropdownMenu'
import DropdownItem from 'reactstrap/lib/DropdownItem'
import logger from 'debug'
const log = logger('app:ui:MultiSelect')

const START_OPEN = false //true
const txt_select_all = 'Alle auswählen'
const txt_all_selected = 'Alle ausgewählt'
const txt_none_selected = 'Keine ausgewählt'
const txt_n_selected_a = ''
const txt_n_selected_e = ` ausgewählt`

// styles
const baseCls = 'grouped-multiselect'
const menuCls = 'py-2 px-0 shadow'
const itemCls = 'py-1 px-2 mb-0 f6 text-wrap'
const groupHeaderCls = 'py-1 px-2 mb-0 f6 font-weight-bold text-wrap'
const groupWrapCls = 'dropdown-item-subgroup'
const groupItemCls = [itemCls, 'pl-4']

class MultiSelect extends React.PureComponent {
  constructor(props) {
    super(props)
    this.state = { dropdownOpen: START_OPEN }
    log('init', { props, state: this.state })
  }

  allOptions = () =>
    f.flatMap(this.props.options, 'options').filter(o => !o.disabled)
  allValues = () => f.map(this.allOptions(), 'value')
  isInactive = () => !!(this.props.readOnly || this.props.disabled)
  selection = () => f.intersection(this.props.value, this.allValues())

  isAllSelected = () => this.allOptions().length === this.selection().length
  groupValues = ({ options }) =>
    f.map(options.filter(o => !o.disabled), 'value')
  groupSelection = group => {
    const selection = this.selection()
    const options = this.groupValues(group)
    const selectedOptions = f.intersection(selection, options)
    return {
      someSelected: !f.isEmpty(selectedOptions),
      allSelected: selectedOptions.length === options.length
    }
  }

  toggleDropdown = () => {
    if (this.props.disabled) return
    this.setState(cur => ({ dropdownOpen: !cur.dropdownOpen }))
  }

  onSelectAllChange = ({ target }) => {
    log('onSelectAllChange', { target })
    const isSelected = target.checked
    const values = !isSelected ? [] : f.map(this.allOptions(), 'value')
    this.onChangeCallback(values)
  }

  onOptionGroupChange = (group, isSelected) => {
    log('onOptionGroupChange', { group, isSelected })
    const selection = this.selection()
    const options = this.groupValues(group)
    const selected = isSelected
      ? f.uniq([...selection, ...options])
      : f.without(selection, ...options)
    this.onChangeCallback(selected)
  }

  onOptionChange = (value, isSelected) => {
    log('onOptionChange', { value, isSelected })
    const selected = [...this.selection(), value].filter(
      val => (val === value ? isSelected : true)
    )
    this.onChangeCallback(selected)
  }

  onChangeCallback = value => {
    const name = this.props.name
    log('onChange', { name, value })
    this.props.onChange({ target: { name, value } })
  }

  render({ props, state } = this) {
    const {
      options,
      name,
      value,
      onChange,
      className,
      block,
      ...restProps
    } = props
    const selection = this.selection()
    const allOptions = this.allOptions()
    const isInactive = this.isInactive()
    const Id = s => `${restProps.id}-${s}`

    return (
      <F>
        <Dropdown
          inNavbar={true} // no `popper`/dynamic placement, just dropdown on bottom
          direction="down"
          size="sm"
          className={cx(className, baseCls)}
          {...restProps}
          isOpen={this.state.dropdownOpen}
          toggle={this.toggleDropdown}
        >
          <DropdownToggle caret outline block={block}>
            <span className="text-wrap">
              {titleBySelection(selection, allOptions)}
            </span>
          </DropdownToggle>

          <DropdownMenu className={cx(menuCls)}>
            <DropdownItem
              tag="label"
              toggle={false}
              className={cx(itemCls)}
              htmlFor={Id('select_all')}
            >
              <Checkbox
                id={Id('select_all')}
                label={txt_select_all}
                checked={this.isAllSelected()}
                onChange={!isInactive && this.onSelectAllChange}
              />
            </DropdownItem>

            <DropdownItem divider />

            {options.map((group, i) => {
              const { label, options } = group
              const cbsaid = Id(`select_group_${label}_{i}`)
              const { someSelected, allSelected } = this.groupSelection(group)

              return (
                <F key={i}>
                  <DropdownItem
                    className={cx(groupHeaderCls)}
                    toggle={false}
                    tag="label"
                    htmlFor={cbsaid}
                  >
                    <Checkbox
                      id={cbsaid}
                      label={label}
                      checked={allSelected}
                      isIndeterminate={someSelected && !allSelected}
                      onChange={
                        !isInactive &&
                        (e => this.onOptionGroupChange(group, !allSelected))
                      }
                    />
                  </DropdownItem>

                  <div className={cx(groupWrapCls)}>
                    {options.map(({ label, value, disabled, ...props }, ii) => {
                      const cbid = Id(`${value}-checkbox-${i}${ii}`)
                      const isSelected = selection.indexOf(value) !== -1
                      const toggle =
                        !isInactive &&
                        (e => this.onOptionChange(value, !isSelected))

                      return (
                        <DropdownItem
                          key={ii}
                          tag="label"
                          toggle={false}
                          className={cx(groupItemCls)}
                          disabled={disabled}
                          htmlFor={cbid}
                        >
                          <Checkbox
                            {...props}
                            id={cbid}
                            label={label}
                            value={value}
                            checked={isSelected}
                            onChange={toggle}
                          />
                        </DropdownItem>
                      )
                    })}
                  </div>
                </F>
              )
            })}
          </DropdownMenu>
        </Dropdown>
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

const Checkbox = ({ id, className, label, isIndeterminate, ...props }) => {
  const setCheckboxRef = checkbox => {
    if (checkbox) {
      checkbox.indeterminate = isIndeterminate || false
    }
  }
  return (
    <div className="custom-control custom-checkbox">
      <input
        type="checkbox"
        tabIndex="-1"
        className={cx(className, 'custom-control-input')}
        id={id}
        ref={setCheckboxRef}
        {...props}
      />
      <label className="custom-control-label" htmlFor={id}>
        {label}
      </label>
    </div>
  )
}

function titleBySelection(selection, allOptions) {
  // Describe None and All, List up to three by Name,
  // otherwise give a count
  const count = selection.length
  const optionsCount = allOptions.length
  if (count === optionsCount) {
    return txt_all_selected
  }
  if (count === 0) {
    return txt_none_selected
  }
  if (count > 3) {
    return `${txt_n_selected_a}${count}/${optionsCount}${txt_n_selected_e}`
  }
  return selection
    .map(item => allOptions.filter(({ value }) => value === item)[0])
    .map(item => item && item.label)
    .join(', ')
}
