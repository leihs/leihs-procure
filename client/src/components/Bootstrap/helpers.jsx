import f from 'lodash'

// build `options` prop for a `<Select/>`, e.g.
// `{id: 23, name: 'Foo'}` to `{value: 23, label: 'Foo'}`
export const optionFromObject = (
  obj,
  path,
  valueKey = 'id',
  labelKey = 'name'
) => {
  const item = path ? f.get(obj.path) : obj
  return { label: f.get(item, labelKey), value: f.get(item, valueKey) }
}

// apply 'optionFromObject' to a list of objects
export const optionsFromList = (list, ...args) =>
  f.map(list, item => optionFromObject(item, ...args))
