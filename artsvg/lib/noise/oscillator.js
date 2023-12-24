/**
 * Oscillator noise
 *
 * Kinda similar to this (https://piterpasma.nl/articles/wobbly) although I had the idea independently
 */

/**
 * @typedef {object} OscillatorAttributes
 * @property {number} period
 * @property {number} amplitude
 * @property {(t: number) => number} [wave=Math.sin]
 * @property {number} [phase=0]
 */

export class Oscillator {
  /** @type {Oscillator[]} */
  #frequencyModulators = []
  /** @type {Oscillator[]} */
  #amplitudeModulators = []
  /** @type {Oscillator[]} */
  #phaseModulators = []
  /** @type {number} */
  #period
  /** @type {number} */
  #frequency
  /** @type {number} */
  #amplitude
  /** @type {(t: number) => number} */
  #wave
  /** @type {number} */
  #phase

  /**
   * @param {OscillatorAttributes} attributes
   */
  constructor({ phase = 0, period, amplitude, wave = Math.sin }) {
    this.#phase = phase
    this.#period = period
    this.#frequency = (2 * Math.PI) / this.#period
    this.#amplitude = amplitude
    this.#wave = wave
  }

  /**
   *
   * @param {number} x
   * @param {number} [y=0]
   * @returns {number}
   */
  frequency(x, y = 0) {
    const modulated = this.#frequencyModulators.reduce(
      (sum, curr) => sum + curr.output(x, y),
      0,
    )
    return this.#frequency + modulated
  }

  /**
   *
   * @param {number} x
   * @param {number} [y=0]
   * @returns {number}
   */
  amplitude(x, y = 0) {
    const modulated = this.#amplitudeModulators.reduce(
      (sum, curr) => sum + curr.output(x, y),
      0,
    )

    // yModulation oscaillates bewteen [-1, 1] on a period of this.#amplitude
    // the purpose of yModulation is to ensure that waves vary over both the x axis and y axis
    const yModulation = Math.sin(y / this.#amplitude)
    return this.#amplitude * yModulation + modulated
    // not sure if this is more appropriate, more experimentation needed
    return (this.#amplitude + modulated) * yModulation
  }

  /**
   *
   * @param {number} x
   * @param {number} [y=0]
   * @returns {number}
   */
  phase(x, y = 0) {
    const modulated = this.#phaseModulators.reduce(
      (sum, curr) => sum + curr.output(x, y),
      0,
    )
    return this.#phase + modulated
  }

  /**
   * @param {Oscillator} osc
   * @returns {Oscillator}
   */
  modulateFrequency(osc) {
    this.#frequencyModulators.push(osc)
    return this
  }

  /**
   * @param {Oscillator} osc
   * @returns {Oscillator}
   */
  modulateAmplitude(osc) {
    this.#amplitudeModulators.push(osc)
    return this
  }

  /**
   * @param {Oscillator} osc
   * @returns {Oscillator}
   */
  modulatePhase(osc) {
    this.#phaseModulators.push(osc)
    return this
  }

  // TODO: need some way to compress/limit this. Otherwise the results will be fairly unpredictable
  // although, maybe it is better to have a compressor as a "plugin" that takes the oscillators output
  // and returns a compressed signal
  // resources: doesn't look remarkably difficult
  // https://midisic.com/compressor-ratio/#:~:text=Output%20%3D%20(Input%20%E2%80%93%20Threshold),the%20calculation%20of%20compressor%20ratio.
  // https://www.audiomasterclass.com/blog/what-is-the-math-behind-audio-compression
  /**
   *
   * @param {number} x
   * @param {number} [y=0]
   * @returns {number}
   */
  output(x, y = 0) {
    return (
      this.#wave(x * this.frequency(x, y) + this.phase(x, y)) *
      this.amplitude(x, y)
    )
  }
}
