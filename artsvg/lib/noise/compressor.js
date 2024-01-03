/**
 * @typedef {object} CompressorOptions
 * @property {number} W the knee. Should be in range [TODO, TODO]
 * @property {number} T the threshold. Should be in range [0, Infinity].
 * @property {number} R the compression ratio. Should be in range [TODO, TODO]
 */

// based on https://dsp.stackexchange.com/questions/73619/how-to-derive-equation-for-second-order-interpolation-of-soft-knee-cutoff-in-a-c
export class Compressor {
  /** @param {CompressorOptions} options */
  constructor({ W, T, R }) {
    this.W = W
    this.T = T
    if (this.T < 0) {
      console.warn(
        `T is '${T}', but was expected to be in range [0, Infinity]. Wonkiness may ensue.`,
      )
    }
    this.R = R
  }
  /**
   * This should be the only public function,
   * but while developing I need to have the other functions available for testing.
   * @param {number} input
   * @returns {number}
   */
  compress(input) {
    if (this.belowKnee(input)) {
      return input
    }
    if (this.insideKnee(input)) {
      return this.compressInsideKnee(input)
    }
    return this.compressAboveKnee(input)
  }

  /**
   * @param {number} input
   * @returns {boolean}
   */
  belowKnee(input) {
    // original formula
    // return 2 * (input - this.T) < -this.W
    return Math.abs(input) < this.T && 2 * (Math.abs(input) - this.T) < -this.W
  }
  /**
   * @param {number} input
   * @returns {boolean}
   */
  insideKnee(input) {
    return 2 * Math.abs(Math.abs(input) - this.T) <= this.W
  }

  /**
   * @param {number} input
   * @returns {number}
   */
  compressInsideKnee(input) {
    const sign = input < 0 ? -1 : 1
    return (
      sign *
      (Math.abs(input) +
        ((1 / this.R - 1) * (Math.abs(input) - this.T + this.W / 2) ** 2) /
          (2 * this.W))
    )
  }

  /**
   * Above the knee, compression is same as "hard knee" compression formula
   * @param {number} input
   * @returns {number}
   */
  compressAboveKnee(input) {
    const sign = input < 0 ? -1 : 1
    return sign * (this.T + (Math.abs(input) - this.T) / this.R)
  }
}
