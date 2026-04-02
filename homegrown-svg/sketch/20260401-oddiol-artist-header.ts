import {
  renderSvg,
  vec2,
  randomSeed,
  PI,
  TAU,
  createOscNoise,
  Random,
  ColorRgb,
  ColorHsl,
  hypot,
} from "@salamivg/core";

// header image
// Spotify artist header min dimensions: 2660x1140px
const config = { width: 2660, height: 1140, scale: 1, loopCount: 1 };
// const config = { width: 800, height: 800, scale: 1, loopCount: 1 };
const REFERENCE_DIM = hypot(800, 800);
const BASE_DIM = hypot(config.width, config.height);
const DIM_SCALE = BASE_DIM / REFERENCE_DIM;
const INV_DIM_SCALE = REFERENCE_DIM / BASE_DIM;

/**
 *
 * BASE SETTINGS
 *
 */
const CLUSTERS_PER_RING = 10;
const PATHS_PER_CLUSTER = 20;
const MAX_ROUNDS = 360;
const CLUSTER_GROWTH_PER_TURN = 40;
const STEP_LENGTH = 2.5 * DIM_SCALE;
const CLUSTER_SEED_RADIUS = 25 * DIM_SCALE;
const CLUSTERS_PER_ACTIVATION_BATCH = 6;
const ACTIVATION_INTERVAL_ROUNDS = 4;
const OUTER_MARGIN = 0;
const TURN_SMOOTHING = 0.35;

/**
 *
 * RING QUANTITY/SPACING
 *
 */
/** larger = more space required between paths */
const COLLISION_CELL_SIZE = 5 * DIM_SCALE;
const STROKE_WIDTH = COLLISION_CELL_SIZE * 0.52;
const CLUSTER_RING_START_RADIUS = BASE_DIM * 0.04;
const CLUSTER_RING_SPACING = BASE_DIM * 0.03;
const CLUSTER_RING_MAX_RADIUS = BASE_DIM * 0.4;
const RING_COUNT = Math.max(
  1,
  Math.floor(
    (CLUSTER_RING_MAX_RADIUS - CLUSTER_RING_START_RADIUS) / CLUSTER_RING_SPACING
  ) + 1
);
const CLUSTER_POSITION_JITTER = BASE_DIM * 0.006;

/**
 *
 * NOISE MIXING
 *
 */
const CLUSTER_NOISE_ANGLE_STRENGTH = PI / 60;
const CLUSTER_NOISE_MIX = 0.35;
const CLUSTER_NOISE_XY_SCALE = [0.015 * INV_DIM_SCALE, 0.035 * INV_DIM_SCALE];
const CLUSTER_NOISE_Z_SCALE = [0.037 * INV_DIM_SCALE, 0.043 * INV_DIM_SCALE];

/**
 *
 *
 * ADAPTIVE DENSITY
 *
 */
const ADAPT_INTERVAL_ROUNDS = 5;
const ADAPT_BIN_COUNT = 18;
const MIN_ACTIVE_PATHS = 16;
const MAX_ACTIVE_PATHS = 52;
const MAX_PATH_ADJUST_PER_ROUND = 8;
const OVERFULL_BIN_LIMIT = 4;
const WIDE_SPREAD_BIN_RATIO = 0.4;
const VERY_WIDE_SPREAD_BIN_RATIO = 0.58;
const PATH_LIGHTNESS_JITTER = 0.06;

/**
 *
 * OUTLINE
 *
 */
const PATH_OUTLINE_COLOR = "#000000";
const PATH_OUTLINE_EXTRA_WIDTH = COLLISION_CELL_SIZE * 0.18;

const colors = [
  "#fcba03",
  "#f5690c",
  "#c0e376",
  "#96e0b9",
  "#b3e6e3",
  "#a2d2f2",
  "#9e98d9",
  "#ba97c9",
  "#d96a82",
].map((c) => ColorRgb.fromHex(c).toHsl());
const WHITE_HSL = ColorRgb.fromHex("#ffffff").toHsl();
const BLACK_HSL = ColorRgb.fromHex("#000000").toHsl();

let seed = randomSeed();
seed = 6991748853577565;

type Point = { x: number; y: number };
type PathCommand = { to: Point; draw: boolean };
type PathState = {
  position: Point;
  heading: number;
  active: boolean;
  lightnessOffset: number;
  commands: PathCommand[];
};
type Cluster = {
  id: number;
  color: ColorHsl;
  center: Point;
  paths: PathState[];
  clusterNoise: (x: number, y: number) => number;
  activationRound: number;
  initialized: boolean;
};

renderSvg(config, (svg) => {
  const rnd = Random.create(seed);
  const canvasCenter = vec2(config.width / 2, config.height / 2);

  svg.filenameMetadata = { seed: String(seed) };
  svg.numericPrecision = 3;
  svg.setBackground("#000000");
  svg.setAttributes({ "stroke-linecap": "round" });

  const clusterCenters = pickClusterCenters(rnd, canvasCenter);
  const clusters: Cluster[] = clusterCenters.map((center, id) => {
    const outwardAngle = angleBetween(canvasCenter, center);
    const clusterColor = rnd.fromArray(colors);
    const clusterNoise = createOscNoise(
      seed + id * 97 + 11,
      rnd.value(CLUSTER_NOISE_XY_SCALE[0], CLUSTER_NOISE_XY_SCALE[1]),
      BASE_DIM * rnd.value(CLUSTER_NOISE_Z_SCALE[0], CLUSTER_NOISE_Z_SCALE[1])
    );
    const paths: PathState[] = [];

    for (let i = 0; i < PATHS_PER_CLUSTER; i++) {
      const spawnAngle = rnd.value(0, TAU);
      const spawnRadius = rnd.value(0, CLUSTER_SEED_RADIUS);
      const start = vec2(
        center.x + Math.cos(spawnAngle) * spawnRadius,
        center.y + Math.sin(spawnAngle) * spawnRadius
      );
      const heading = outwardAngle;
      const path: PathState = {
        position: start,
        heading,
        active: false,
        lightnessOffset: rnd.value(
          -PATH_LIGHTNESS_JITTER,
          PATH_LIGHTNESS_JITTER
        ),
        commands: [{ to: start, draw: false }],
      };
      paths.push(path);
    }

    return {
      id,
      color: clusterColor,
      center,
      paths,
      clusterNoise,
      activationRound: 0,
      initialized: false,
    };
  });
  const clustersRandomized = rnd.shuffle([...clusters]);
  assignActivationRounds(clusters, rnd);

  for (let round = 0; round < MAX_ROUNDS; round++) {
    for (const cluster of clustersRandomized) {
      if (!cluster.initialized && round >= cluster.activationRound) {
        initializeCluster(cluster);
      }
      if (!cluster.initialized) {
        continue;
      }
      for (let step = 0; step < CLUSTER_GROWTH_PER_TURN; step++) {
        growClusterStep(cluster);
      }
      if (round % ADAPT_INTERVAL_ROUNDS === 0) {
        adaptClusterDensity(cluster);
      }
    }
  }

  for (const cluster of clustersRandomized) {
    for (const state of cluster.paths) {
      const outlineCommands = withLeadingCap(
        state.commands,
        PATH_OUTLINE_EXTRA_WIDTH
      );
      svg.path((p) => {
        p.fill = "none";
        p.stroke = PATH_OUTLINE_COLOR;
        p.strokeWidth = STROKE_WIDTH + PATH_OUTLINE_EXTRA_WIDTH;

        for (let i = 0; i < outlineCommands.length; i++) {
          const cmd = outlineCommands[i];
          if (i === 0 || !cmd.draw) {
            p.moveTo(vec2(cmd.to.x, cmd.to.y));
          } else {
            p.lineTo(vec2(cmd.to.x, cmd.to.y));
          }
        }
      });

      svg.path((p) => {
        p.fill = "none";
        p.stroke = applyLightnessOffset(cluster.color, state.lightnessOffset);
        p.strokeWidth = STROKE_WIDTH;

        for (let i = 0; i < state.commands.length; i++) {
          const cmd = state.commands[i];
          if (i === 0 || !cmd.draw) {
            p.moveTo(vec2(cmd.to.x, cmd.to.y));
          } else {
            p.lineTo(vec2(cmd.to.x, cmd.to.y));
          }
        }
      });
    }
  }

  return () => {
    seed = randomSeed();
  };

  function growClusterStep(cluster: Cluster) {
    for (let pathIndex = 0; pathIndex < cluster.paths.length; pathIndex++) {
      const state = cluster.paths[pathIndex];
      if (!state.active) {
        continue;
      }

      const outward = angleBetween(canvasCenter, state.position);
      const clusterHeading =
        outward +
        cluster.clusterNoise(state.position.x, state.position.y) *
          CLUSTER_NOISE_ANGLE_STRENGTH;
      const targetHeading = interpolateAngle(
        outward,
        clusterHeading,
        CLUSTER_NOISE_MIX
      );
      state.heading = interpolateAngle(
        state.heading,
        targetHeading,
        TURN_SMOOTHING
      );

      const next = vec2(
        state.position.x + Math.cos(state.heading) * STEP_LENGTH,
        state.position.y + Math.sin(state.heading) * STEP_LENGTH
      );

      if (!inBounds(next)) {
        state.active = false;
        continue;
      }

      state.commands.push({ to: next, draw: true });
      state.position = next;
    }
  }

  function initializeCluster(cluster: Cluster) {
    cluster.initialized = true;
    for (const state of cluster.paths) {
      if (!inBounds(state.position)) {
        const relocated = findFreeStart(cluster.center);
        if (!relocated) {
          state.active = false;
          continue;
        }
        state.position = relocated;
        state.commands = [{ to: relocated, draw: false }];
      }
      state.active = true;
    }
  }

  function findFreeStart(center: Point): Point | null {
    const attempts = 24;
    for (let i = 0; i < attempts; i++) {
      const angle = rnd.value(0, TAU);
      const radius = rnd.value(0, CLUSTER_SEED_RADIUS);
      const p = vec2(
        center.x + Math.cos(angle) * radius,
        center.y + Math.sin(angle) * radius
      );
      if (!inBounds(p)) {
        continue;
      }
      return p;
    }
    return null;
  }

  function adaptClusterDensity(cluster: Cluster) {
    const active = cluster.paths.filter((p) => p.active);
    if (active.length === 0) {
      return;
    }

    const bins = Array.from(
      { length: ADAPT_BIN_COUNT },
      () => [] as PathState[]
    );
    for (const state of active) {
      const a = normalizeAngle(angleBetween(cluster.center, state.position));
      bins[binForAngle(a)].push(state);
    }

    let removedCount = 0;
    for (const bin of bins) {
      if (bin.length <= OVERFULL_BIN_LIMIT) {
        continue;
      }
      const removeN = Math.min(
        bin.length - OVERFULL_BIN_LIMIT,
        MAX_PATH_ADJUST_PER_ROUND - removedCount
      );
      for (let i = 0; i < removeN; i++) {
        const victim = bin.pop();
        if (victim) {
          victim.active = false;
          removedCount += 1;
        }
      }
      if (removedCount >= MAX_PATH_ADJUST_PER_ROUND) {
        break;
      }
    }

    const occupiedBins = bins.filter((bin) => bin.length > 0).length;
    const spreadRatio = occupiedBins / ADAPT_BIN_COUNT;
    let bonusAdd = 0;
    if (spreadRatio > WIDE_SPREAD_BIN_RATIO) {
      bonusAdd += 1;
    }
    if (spreadRatio > VERY_WIDE_SPREAD_BIN_RATIO) {
      bonusAdd += 1;
    }

    const activeAfterRemoval = cluster.paths.filter((p) => p.active).length;
    const floorGap = Math.max(0, MIN_ACTIVE_PATHS - activeAfterRemoval);
    let addBudget = Math.min(
      MAX_PATH_ADJUST_PER_ROUND,
      removedCount + bonusAdd + floorGap
    );

    if (activeAfterRemoval + addBudget > MAX_ACTIVE_PATHS) {
      addBudget = Math.max(0, MAX_ACTIVE_PATHS - activeAfterRemoval);
    }
    if (addBudget <= 0) {
      return;
    }

    const candidateBins = rankedSpawnBins(bins);
    for (let i = 0; i < candidateBins.length && addBudget > 0; i++) {
      const spawned = spawnPathInBin(cluster, candidateBins[i]);
      if (spawned) {
        addBudget -= 1;
      }
    }
  }

  function rankedSpawnBins(bins: PathState[][]): number[] {
    const candidates: Array<{ idx: number; score: number }> = [];
    for (let i = 0; i < ADAPT_BIN_COUNT; i++) {
      const left = bins[(i - 1 + ADAPT_BIN_COUNT) % ADAPT_BIN_COUNT].length;
      const here = bins[i].length;
      const right = bins[(i + 1) % ADAPT_BIN_COUNT].length;
      const neighborhood = left + right;
      if (neighborhood === 0) {
        continue;
      }
      const underfill = Math.max(0, OVERFULL_BIN_LIMIT - here);
      const score = neighborhood * 2 + underfill;
      if (score > 0) {
        candidates.push({ idx: i, score });
      }
    }
    candidates.sort((a, b) => b.score - a.score);
    return candidates.map((c) => c.idx);
  }

  function spawnPathInBin(cluster: Cluster, binIndex: number): boolean {
    const binStart = (binIndex / ADAPT_BIN_COUNT) * TAU;
    const binSize = TAU / ADAPT_BIN_COUNT;
    const attempts = 12;
    for (let i = 0; i < attempts; i++) {
      const angle = binStart + rnd.value(0.18, 0.82) * binSize;
      const radius = rnd.value(
        CLUSTER_SEED_RADIUS * 0.2,
        CLUSTER_SEED_RADIUS * 1.05
      );
      const start = vec2(
        cluster.center.x + Math.cos(angle) * radius,
        cluster.center.y + Math.sin(angle) * radius
      );
      if (!inBounds(start)) {
        continue;
      }

      const heading = angleBetween(canvasCenter, start);

      cluster.paths.push({
        position: start,
        heading,
        active: true,
        lightnessOffset: rnd.value(
          -PATH_LIGHTNESS_JITTER,
          PATH_LIGHTNESS_JITTER
        ),
        commands: [{ to: start, draw: false }],
      });
      return true;
    }
    return false;
  }

  function inBounds(p: Point): boolean {
    return (
      p.x >= OUTER_MARGIN &&
      p.x <= config.width - OUTER_MARGIN &&
      p.y >= OUTER_MARGIN &&
      p.y <= config.height - OUTER_MARGIN
    );
  }

  function applyLightnessOffset(base: ColorHsl, offset: number): ColorHsl {
    if (offset >= 0) {
      return base.mix(WHITE_HSL, offset);
    }
    return base.mix(BLACK_HSL, -offset);
  }

  function withLeadingCap(
    commands: PathCommand[],
    capLength: number
  ): PathCommand[] {
    if (commands.length < 2 || capLength <= 0) {
      return commands;
    }
    for (let i = 1; i < commands.length; i++) {
      const current = commands[i];
      if (!current.draw) {
        continue;
      }
      const from = commands[i - 1].to;
      const to = current.to;
      const dx = to.x - from.x;
      const dy = to.y - from.y;
      const len = Math.hypot(dx, dy);
      if (len <= 1e-6) {
        continue;
      }
      const capStart = vec2(
        from.x - (dx / len) * capLength,
        from.y - (dy / len) * capLength
      );
      return [
        { to: capStart, draw: false },
        { to: from, draw: true },
        ...commands,
      ];
    }
    return commands;
  }
});

function assignActivationRounds(clusters: Cluster[], rnd: Random) {
  const order = clusters.map((_, index) => index);
  for (let i = order.length - 1; i > 0; i--) {
    const j = Math.floor(rnd.value(0, i + 1));
    const tmp = order[i];
    order[i] = order[j];
    order[j] = tmp;
  }
  for (let i = 0; i < order.length; i++) {
    const batch = Math.floor(i / CLUSTERS_PER_ACTIVATION_BATCH);
    clusters[order[i]].activationRound = batch * ACTIVATION_INTERVAL_ROUNDS;
  }
}

function pickClusterCenters(rnd: Random, center: Point): Point[] {
  const centers: Point[] = [];
  for (let ringIndex = 0; ringIndex < RING_COUNT; ringIndex++) {
    const radius = CLUSTER_RING_START_RADIUS + ringIndex * CLUSTER_RING_SPACING;
    const ringClusterCount = CLUSTERS_PER_RING * (ringIndex + 1);
    const phaseOffset =
      (ringIndex % 2 === 0 ? 0 : 0.5) * (TAU / ringClusterCount);
    for (let i = 0; i < ringClusterCount; i++) {
      const angle = (i / ringClusterCount) * TAU + phaseOffset;
      const base = vec2(
        center.x + Math.cos(angle) * radius,
        center.y + Math.sin(angle) * radius
      );
      centers.push(base.jitter(CLUSTER_POSITION_JITTER, rnd.rng));
    }
  }

  return centers;
}

function interpolateAngle(current: number, target: number, t: number): number {
  let delta = target - current;
  while (delta > PI) delta -= TAU;
  while (delta < -PI) delta += TAU;
  return current + delta * t;
}

function normalizeAngle(angle: number): number {
  const a = angle % TAU;
  return a < 0 ? a + TAU : a;
}

function binForAngle(angle: number): number {
  const normalized = normalizeAngle(angle);
  const raw = Math.floor((normalized / TAU) * ADAPT_BIN_COUNT);
  return Math.max(0, Math.min(ADAPT_BIN_COUNT - 1, raw));
}

function angleBetween(from: Point, to: Point): number {
  return Math.atan2(to.y - from.y, to.x - from.x);
}

function distance(a: Point, b: Point): number {
  return Math.hypot(b.x - a.x, b.y - a.y);
}
