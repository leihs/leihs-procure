// global setup for test
// must do at least the side-effecty stuff from './index.js'

import f from 'lodash'
import lodashMixins from './lodash-mixins'

f.mixin(lodashMixins)
