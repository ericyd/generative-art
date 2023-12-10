/**
 * stuff in this file copy/pasted from
 * https://stackoverflow.com/questions/521295/seeding-the-random-number-generator-in-javascript
 */

export function cyrb128(str) {
  let h1 = 1779033703,
    h2 = 3144134277,
    h3 = 1013904242,
    h4 = 2773480762;
  for (let i = 0, k; i < str.length; i++) {
    k = str.charCodeAt(i);
    h1 = h2 ^ Math.imul(h1 ^ k, 597399067);
    h2 = h3 ^ Math.imul(h2 ^ k, 2869860233);
    h3 = h4 ^ Math.imul(h3 ^ k, 951274213);
    h4 = h1 ^ Math.imul(h4 ^ k, 2716044179);
  }
  h1 = Math.imul(h3 ^ (h1 >>> 18), 597399067);
  h2 = Math.imul(h4 ^ (h2 >>> 22), 2869860233);
  h3 = Math.imul(h1 ^ (h3 >>> 17), 951274213);
  h4 = Math.imul(h2 ^ (h4 >>> 19), 2716044179);
  return [
    (h1 ^ h2 ^ h3 ^ h4) >>> 0,
    (h2 ^ h1) >>> 0,
    (h3 ^ h1) >>> 0,
    (h4 ^ h1) >>> 0,
  ];
}

export function sfc32(a, b, c, d) {
  return function () {
    a >>>= 0;
    b >>>= 0;
    c >>>= 0;
    d >>>= 0;
    var t = (a + b) | 0;
    a = b ^ (b >>> 9);
    b = (c + (c << 3)) | 0;
    c = (c << 21) | (c >>> 11);
    d = (d + 1) | 0;
    t = (t + d) | 0;
    c = (c + t) | 0;
    return (t >>> 0) / 4294967296;
  };
}

/**
 * @callback Rng
 * @returns {number} in range [0, 1]
 */

/**
 *
 * @param {string} seed
 * @returns {Rng}
 */
export function createRng(seed) {
  var seed = cyrb128(String(seed) ?? String(Date.now()));
  // Four 32-bit component hashes provide the seed for sfc32.
  return sfc32(seed[0], seed[1], seed[2], seed[3]);
}

/**
 * @param {number} min 
 * @param {number} max 
 * @param {Rng} rng 
 * @returns {number}
 */
export function random(min, max, rng = Math.random) {
  if (min > max) {
    [min, max] = [max, min];
  } // swap values
  return min + rng() * (max - min);
}

/**
 * @param {number} min 
 * @param {number} max 
 * @param {Rng} rng 
 * @returns {number} an integer
 */
export function randomInt(min, max, rng = Math.random) {
  return Math.floor(random(min, max, rng))
}

/**
 * @param {Rng} rng 
 * @returns {number} an integer in range [0, Number.MAX_SAFE_INTEGER]
 */
export function randomSeed(rng = Math.random) {
  return Math.floor(random(0, Number.MAX_SAFE_INTEGER, rng))
}

/**
 * Returns a random number in range [`value` - `amount`, `value` + `amount`]
 * @param {number} amount
 * @param {number} value
 * @param {Rng} rng 
 * @returns {number}
 */
export function jitter(amount, value, rng) {
  return random(value - amount, value + amount, rng);
}

/**
 * Shuffle an array.
 * Returns a new array, does *not* modify in place.
 * @param {Array<T>} arr
 * @param {Rng} rng 
 * @returns {Array<T>}
 */
export function shuffle(arr, rng = Math.random) {
  const copy = [...arr]; // create a copy of original array
  for (let i = copy.length - 1; i; i--) {
    const randomIndex = Math.floor(random(0, i + 1, rng));
    [copy[i], copy[randomIndex]] = [copy[randomIndex], copy[i]]; // swap
  }
  return copy;
}

/**
 * Returns a random value from an array
 * @param {Array<T>} array 
 * @param {Rng} rng 
 * @returns {T}
 */
export function randomFromArray(array, rng = Math.random) {
  const index = randomInt(0, array.length, rng)
  return array[index]
}
