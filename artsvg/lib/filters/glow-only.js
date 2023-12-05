import { Filter, tag } from '../tag.js'

// same as #glow but does not blend with source graphic
export const glowOnly = Filter(
  {
    id: 'glow-only',
    filterUnits: 'userSpaceOnUse',
    primitiveUnits: 'userSpaceOnUse',
  },
  [
    tag('feMorphology', {
      id: 'morphology',
      operator: 'dilate',
      radius: '4.5',
      in: 'SourceGraphic',
      result: 'thicken',
    }),
    tag('feGaussianBlur', {
      id: 'gaussian',
      stdDeviation: '4.5',
      in: 'thicken',
      result: 'coloredBlur',
    }),
  ],
)
