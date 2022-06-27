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
import { vec2, Vector2 } from "./Vector2.js";

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
 * @property {() => Point} cursor get the current point
 * @property {(endPoint: Point, coordinateType: CoordinateType) => void} move lift up pen and move to point
 * @property {() => void} close close the path back to the start point
 * @property {(endPoint: Point, coodinateType: CoordinateType) => void} line straight line to point
 * @property {(controlPoint1: Point, controlPoint2: Point, endPoint: Point, coordinateType: CoordinateType) => void} cubicBezier
 * @property {(controlPoint: Point, endPoint: Point, coordinateType: CoordinateType) => void} smoothBezier
 */

/**
 * @typedef Radians
 * @type {number}
 */

export class PathInstruction {
  /**
   *
   * @param {'l' | 'L' | 'm' | 'M' | 'c' | 'C' | 's' | 'S'} commandType
   * @param {Vector2[]} points
   */
  constructor(commandType, points) {
    this.endPoint = points?.[0] ?? vec2(0, 0);
    this.points = points;
    this.commandType = commandType;
  }

  toString() {
    return [
      this.commandType,
      ...this.points.map((pt) => [pt.x, pt.y].join(" ")),
    ].join(" ");
  }
}

/**
 *
 * @param {Point} endPoint
 * @param {CoordinateType} coordinateType
 * @returns {PathInstruction}
 */
export function move(endPoint, coordinateType = "absolute") {
  return new PathInstruction(coordinateType === "absolute" ? "M" : "m", [
    endPoint,
  ]);
}

/**
 *
 * @param {Point} endPoint
 * @param {CoordinateType} coordinateType
 * @returns {PathInstruction}
 */
export function line(endPoint, coordinateType = "absolute") {
  return new PathInstruction(coordinateType === "absolute" ? "L" : "l", [
    endPoint,
  ]);
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
 * @returns {PathInstruction}
 */
export function cubicBezier(
  controlPoint1,
  controlPoint2,
  endPoint,
  coordinateType = "absolute"
) {
  return new PathInstruction(coordinateType === "absolute" ? "C" : "c", [
    controlPoint1,
    controlPoint2,
    endPoint,
  ]);
}

/**
 * S
 * Draw a smooth cubic Bézier curve from the current point to the end point specified by x,y.
 * The end control point is specified by x2,y2.
 * The start control point is a reflection of the end control point of the previous curve command
 * @param {Point} controlPoint
 * @param {Point} endPoint
 * @param {'absolute' | 'relative'} coordinateType
 * @returns {PathInstruction}
 */
export function smoothBezier(
  controlPoint,
  endPoint,
  coordinateType = "absolute"
) {
  return new PathInstruction(coordinateType === "absolute" ? "S" : "s", [
    controlPoint,
    endPoint,
  ]);
}

export function close() {
  return new PathInstruction("Z", []);
}

// TODO
// Quadratic Bézier Curve: Q, q, T, t
// Elliptical Arc Curve: A, a

/**
 * @deprecated use vec2
 * @param {number} x
 * @param {number} y
 * @returns {Vector2}
 */
export function point(x, y) {
  return vec2(x, y);
}

/**
 * @deprecated use Vector2.random
 * @param {number} xMin
 * @param {number} xMax
 * @param {number} yMin
 * @param {number} yMax
 * @returns {Point}
 */
export function randomPoint(xMin, xMax, yMin, yMax, rng) {
  return Vector2.random(xMin, xMax, yMin, yMax, rng);
}

/**
 *
 * @param {(pathBuilder: PathBuilder, cursor: Point) => void} drawingCallback
 * @returns string
 */
export function pathBuilder(drawingCallback) {
  // dang, this doesn't work when passing to the callback.
  // I guess I'll have to lean on `relative` movements instead of a `cursor` concept :\
  let cursor = { x: 0, y: 0 };
  const path = [];

  const newCursor = (coordinateType, cursor, endPoint) =>
    coordinateType === "absolute"
      ? endPoint
      : point(cursor.x + endPoint.x, cursor.y + endPoint.y);

  const pathBuilder = {
    cursor: () => cursor,
    move: (endPoint, coordinateType) => {
      path.push(move(endPoint, coordinateType));
      cursor = newCursor(coordinateType, cursor, endPoint);
    },
    line: (endPoint, coordinateType) => {
      path.push(line(endPoint, coordinateType));
      cursor = newCursor(coordinateType, cursor, endPoint);
    },
    close: () => {
      path.push(close());
    },
    cubicBezier: (controlPoint1, controlPoint2, endPoint, coordinateType) => {
      path.push(
        cubicBezier(controlPoint1, controlPoint2, endPoint, coordinateType)
      );
      cursor = newCursor(coordinateType, cursor, endPoint);
    },
    smoothBezier: (controlPoint, endPoint, coordinateType) => {
      path.push(smoothBezier(controlPoint, endPoint, coordinateType));
      cursor = newCursor(coordinateType, cursor, endPoint);
    },
  };
  drawingCallback(pathBuilder);
  // with pathInstructions
  return path.map((p) => p.toString()).join(" ");
}
