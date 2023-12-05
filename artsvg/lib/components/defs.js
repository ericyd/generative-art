import { Tag } from './tag.js'

export class Defs extends Tag {
  constructor() {
    super('defs')
  }
}

export function defs() {
  return new Defs()
}
