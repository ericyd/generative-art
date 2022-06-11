// utilities for svg path tags
// https://developer.mozilla.org/en-US/docs/Web/SVG/Attribute/d#path_commands
//
// MoveTo: M, m
// LineTo: L, l, H, h, V, v
// Cubic Bézier Curve: C, c, S, s
// Quadratic Bézier Curve: Q, q, T, t
// Elliptical Arc Curve: A, a
// ClosePath: Z, z

// Note: Commands are case-sensitive.
// An upper-case command specifies absolute coordinates,
// while a lower-case command specifies coordinates relative to the current position.

// It is always possible to specify a negative value as an argument to a command:
//   negative angles will be anti-clockwise;
//   absolute negative x and y values are interpreted as negative coordinates;
//   relative negative x values move to the left, and relative negative y values move upwards.

import { random } from "./util.js";

/**
 * @typedef Point
 * @type {object}
 * @property {number} x
 * @property {number} y
 */

/**
 * @typedef CoordinateType
 * @type {'absolute' | 'relative'}
 */

/**
 * @typedef PathBuilder
 * @type {object}
 * @property {(endPoint: Point, coordinateType: CoordinateType) => void} move lift up pen and move to point
 * @property {() => void} close close the path back to the start point
 * @property {(controlPoint1: Point, controlPoint2: Point, endPoint: Point, coordinateType: CoordinateType) => void} cubicBezier
 * @property {(controlPoint: Point, endPoint: Point, coordinateType: CoordinateType) => void} smoothBezier
 */

/**
 *
 * @param {Point} endPoint
 * @param {CoordinateType} coordinateType
 * @returns string
 */
export function move(endPoint, coordinateType = "absolute") {
  return [
    coordinateType === "absolute" ? "M" : "m",
    endPoint.x,
    endPoint.y,
  ].join(" ");
}

/**
 *
 * @param {Point} endPoint
 * @param {CoordinateType} coordinateType
 * @returns string
 */
export function line(endPoint, coordinateType = "absolute") {
  return [
    coordinateType === "absolute" ? "L" : "l",
    endPoint.x,
    endPoint.y,
  ].join(" ");
}

/**
 * * C
 * (x1,y1, x2,y2, x,y)
 * Draw a cubic Bézier curve from the current point to the end point specified by x,y.
 * The start control point is specified by x1,y1 and the end control point is specified by x2,y2
 * @param {Point} controlPoint1
 * @param {Point} controlPoint2
 * @param {Point} endPoint
 * @param {CoordinateType} coordinateType
 */
export function cubicBezier(
  controlPoint1,
  controlPoint2,
  endPoint,
  coordinateType = "absolute"
) {
  return [
    coordinateType === "absolute" ? "C" : "c",
    controlPoint1.x,
    controlPoint1.y,
    controlPoint2.x,
    controlPoint2.y,
    endPoint.x,
    endPoint.y,
  ].join(" ");
}

/**
 * S
 * Draw a smooth cubic Bézier curve from the current point to the end point specified by x,y.
 * The end control point is specified by x2,y2.
 * The start control point is a reflection of the end control point of the previous curve command
 * @param {Point} controlPoint
 * @param {Point} endPoint
 * @param {'absolute' | 'relative'} coordinateType
 * @returns string
 */
export function smoothBezier(
  controlPoint,
  endPoint,
  coordinateType = "absolute"
) {
  return [
    coordinateType === "absolute" ? "S" : "s",
    controlPoint.x,
    controlPoint.y,
    endPoint.x,
    endPoint.y,
  ].join(" ");
}

export function close() {
  return "Z";
}

// TODO
// Quadratic Bézier Curve: Q, q, T, t
// Elliptical Arc Curve: A, a

/**
 *
 * @param {number} x
 * @param {number} y
 * @returns Point
 */
export function point(x, y) {
  return { x, y };
}

/**
 *
 * @param {number} xMin
 * @param {number} xMax
 * @param {number} yMin
 * @param {number} yMax
 * @returns Point
 */
export function randomPoint(xMin, xMax, yMin, yMax, rng) {
  return point(random(xMin, xMax, rng), random(yMin, yMax, rng));
}

/**
 *
 * @param {(pathBuilder: PathBuilder, cursor: Point) => void} drawingCallback
 * @returns string
 */
export function pathBuilder(drawingCallback) {
  let cursor = { x: 0, y: 0 };
  const path = [];
  const pathBuilder = {
    move: (endPoint, coordinateType) => {
      path.push(move(endPoint, coordinateType));
      cursor = endPoint;
    },
    close: () => {
      path.push(close());
    },
    cubicBezier: (controlPoint1, controlPoint2, endPoint, coordinateType) => {
      path.push(
        cubicBezier(controlPoint1, controlPoint2, endPoint, coordinateType)
      );
      cursor = endPoint;
    },
    smoothBezier: (controlPoint, endPoint, coordinateType) => {
      path.push(smoothBezier(controlPoint, endPoint, coordinateType));
      cursor = endPoint;
    },
  };
  drawingCallback(pathBuilder, cursor);
  return path.join(" ");
}
