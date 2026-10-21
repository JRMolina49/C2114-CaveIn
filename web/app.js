"use strict";

const scene = document.getElementById("scene");
const cover = document.getElementById("transition-cover");
const transitionArt = document.getElementById("transition-art");
const screens = {
  title: document.getElementById("title-screen"),
  howto: document.getElementById("howto-screen"),
  round: document.getElementById("round-screen"),
  game: document.getElementById("game-screen"),
  winner: document.getElementById("winner-screen")
};

const seatStates = ["grey-human", "blue-human", "green-human", "yellow-human",
  "blue-bot", "green-bot", "yellow-bot"];
const seats = ["red-human", "grey-human", "grey-human", "grey-human"];
const humanNames = ["", "", "", ""];
let nameIndex = 0;
let gameState = null;
let waiting = false;

const assets = {
  red: "assets/player-red.svg",
  blue: "assets/player-blue.svg",
  green: "assets/player-green.svg",
  yellow: "assets/player-yellow.svg"
};

const dieOutcomes = {
  green: new Set(["gem", "footprint"]),
  yellow: new Set(["gem", "footprint", "rockfall"]),
  red: new Set(["gem", "rockfall"])
};

function resizeScene() {
  const scale = Math.min(window.innerWidth / 1440, window.innerHeight / 1024);
  scene.style.transform = `translate(-50%, -50%) scale(${scale})`;
}

window.addEventListener("resize", resizeScene);
resizeScene();

function showScreen(name) {
  for (const [key, element] of Object.entries(screens)) {
    element.hidden = key !== name;
  }
}

function delay(milliseconds) {
  return new Promise(resolve => window.setTimeout(resolve, milliseconds));
}

async function flickerTransition(onReveal) {
  scene.classList.add("flicker");
  await delay(700);
  scene.classList.remove("flicker");
  cover.classList.add("active");
  transitionArt.classList.remove("visible");
  transitionArt.removeAttribute("src");
  await delay(2000);
  transitionArt.src = "assets/blank-screen.svg";
  await transitionArt.decode();
  transitionArt.classList.add("visible");
  await delay(350);
  transitionArt.classList.remove("visible");
  await delay(180);
  transitionArt.src = "assets/lights-on-screen.svg";
  await transitionArt.decode();
  transitionArt.classList.add("visible");
  await delay(450);
  onReveal();
  cover.classList.remove("active");
  transitionArt.classList.remove("visible");
  transitionArt.removeAttribute("src");
}

function stateParts(state) {
  const dash = state.lastIndexOf("-");
  return { color: state.slice(0, dash), kind: state.slice(dash + 1) };
}

function usedColor(exceptIndex) {
  return new Set(seats
    .filter((_, index) => index !== exceptIndex)
    .map(stateParts)
    .filter(part => part.color !== "grey")
    .map(part => part.color));
}

function nextSeatState(index) {
  const used = usedColor(index);
  const currentIndex = seatStates.indexOf(seats[index]);
  for (let step = 1; step <= seatStates.length; step++) {
    const candidate = seatStates[(currentIndex + step) % seatStates.length];
    const { color } = stateParts(candidate);
    if (color === "grey" || !used.has(color)) {
      return candidate;
    }
  }
  return "grey-human";
}

function renderSeats() {
  const selector = document.getElementById("seat-selector");
  selector.replaceChildren();
  seats.forEach((state, index) => {
    const { color, kind } = stateParts(state);
    const button = document.createElement("button");
    button.type = "button";
    button.className = `seat-button seat-${color}`;
    button.dataset.state = state;
    button.setAttribute("aria-label", index === 0
      ? "Required red human player"
      : `Seat ${index + 1}: ${color} ${kind}; click to change`);
    button.disabled = index === 0;
    button.innerHTML = '<span class="seat-head"></span><span class="seat-body"></span>';
    if (index !== 0) {
      button.addEventListener("click", () => {
        const nextState = nextSeatState(index);
        if (nextState !== seats[index]) {
          seats[index] = nextState;
          humanNames.fill("");
          nameIndex = 0;
        }
        updateNamePlaceholder();
        renderSeats();
      });
    }
    selector.append(button);
  });
}

function humanSeatIndices() {
  return seats
    .map((state, index) => ({ state, index }))
    .filter(entry => entry.state !== "grey-human"
      && stateParts(entry.state).kind === "human")
    .map(entry => entry.index);
}

function updateNamePlaceholder() {
  const input = document.getElementById("player-name");
  const indices = humanSeatIndices();
  nameIndex = Math.min(nameIndex, indices.length - 1);
  const seatNumber = indices.indexOf(indices[nameIndex]) + 1;
  input.placeholder = `Enter player ${seatNumber}'s name`;
  input.value = humanNames[nameIndex] || "";
  document.getElementById("setup-message").textContent = "";
}

renderSeats();
updateNamePlaceholder();

document.getElementById("player-name").addEventListener("input", event => {
  humanNames[nameIndex] = event.target.value;
});

document.getElementById("player-name").addEventListener("keydown", event => {
  if (event.key !== "Enter") {
    return;
  }
  event.preventDefault();
  const indices = humanSeatIndices();
  if (humanNames[nameIndex].trim() && nameIndex < indices.length - 1) {
    nameIndex++;
    updateNamePlaceholder();
  }
});

async function request(path, values = {}) {
  const response = await fetch(path, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8" },
    body: new URLSearchParams(values)
  });
  const result = await response.json();
  if (!response.ok) {
    throw new Error(result.error || `Request failed (${response.status}).`);
  }
  return result;
}

function setupValues() {
  const indices = humanSeatIndices();
  const values = {};
  seats.forEach((state, index) => {
    values[`slot${index}`] = state;
  });
  for (let i = 0; i < indices.length; i++) {
    const name = humanNames[i].trim();
    if (!name) {
      nameIndex = i;
      updateNamePlaceholder();
      document.getElementById("player-name").focus();
      throw new Error(`Enter a name for player ${i + 1}.`);
    }
    values[`name${i}`] = name;
  }
  const activeCount = seats.filter(state => state !== "grey-human").length;
  if (activeCount < 2) {
    throw new Error("Choose at least one more player.");
  }
  return values;
}

document.getElementById("start-button").addEventListener("click", async () => {
  if (waiting) {
    return;
  }
  const message = document.getElementById("setup-message");
  try {
    const values = setupValues();
    waiting = true;
    const response = await request("/api/start", values);
    updateGame(response.state);
    message.textContent = "";
    await flickerTransition(() => {
      showScreen("howto");
    });
  } catch (error) {
    message.textContent = error.message;
  } finally {
    waiting = false;
  }
});

document.getElementById("next-button").addEventListener("click", async event => {
  if (waiting) {
    return;
  }
  waiting = true;
  event.currentTarget.disabled = true;
  await flickerTransition(() => {
    showScreen("round");
    renderRoundAnnouncement([]);
  });
  waiting = false;
  await delay(2000);
  await beginTurn();
});

function renderRoundAnnouncement(events) {
  if (!gameState) {
    return;
  }
  document.getElementById("round-number").textContent = gameState.round;
  document.getElementById("turn-announcement").textContent =
    `${gameState.currentPlayer}'s TURN`;
  const details = events.map(event => {
    if (event.cavedIn) {
      return `${event.name} caved in and lost ${event.gemsCollected} gems.`;
    }
    return `${event.name} banked ${event.banked} gems.`;
  });
  document.getElementById("bot-announcement").textContent = details.join("  ");
}

async function beginTurn(internal = false) {
  if (!gameState || gameState.gameOver || (waiting && !internal)) {
    return;
  }
  waiting = true;
  try {
    await flickerTransition(() => showScreen("game"));
    const response = await request("/api/begin-turn");
    await presentRoll(response, true);
  } catch (error) {
    displayError(error);
  } finally {
    waiting = false;
  }
}

function updateGame(state) {
  gameState = state;
  if (!state.started) {
    return;
  }
  document.getElementById("game-round").textContent = state.round;
  document.getElementById("game-player").textContent = state.currentPlayer || "";
  document.getElementById("gems-count").textContent = state.gemsInHand;
  document.getElementById("risk-count").textContent = `${state.riskPercent}%`;
  document.getElementById("risk-fill").style.width = `${state.riskPercent}%`;
  document.getElementById("dig-button").disabled = !state.canDig || state.gameOver;
  document.getElementById("bank-button").disabled = !state.canBank || state.gameOver;
  const scoreboard = document.getElementById("scoreboard");
  scoreboard.replaceChildren();
  for (const player of state.players) {
    const row = document.createElement("div");
    row.className = `score-row${player.current ? " current" : ""}`;
    const icon = document.createElement("img");
    icon.src = assets[player.color] || assets.red;
    icon.alt = "";
    const copy = document.createElement("div");
    copy.className = "score-copy";
    const name = document.createElement("span");
    name.className = "score-name";
    name.textContent = player.name + (player.bot ? " · BOT" : "");
    const total = document.createElement("span");
    total.className = "score-total";
    total.textContent = `${player.score} gems`;
    copy.append(name, total);
    row.append(icon, copy);
    scoreboard.append(row);
  }
  const rocks = document.getElementById("rockfall-count");
  rocks.replaceChildren();
  for (let i = 0; i < state.rockfalls; i++) {
    const icon = document.createElement("img");
    icon.src = "assets/rockfall-red.svg";
    icon.alt = "Rockfall";
    rocks.append(icon);
  }
}

function resultAsset(result) {
  if (!dieOutcomes[result.color]?.has(result.face)) {
    throw new Error(`Invalid ${result.color} die outcome: ${result.face}.`);
  }
  if (result.face === "gem") {
    return `assets/gem-${result.color}.svg`;
  }
  if (result.face === "footprint") {
    return "assets/footprint-icon.svg";
  }
  return "assets/rockfall-icon.svg";
}

function renderRoll(results) {
  const container = document.getElementById("roll-results");
  container.replaceChildren();
  results.forEach((result, index) => {
    const die = document.createElement("div");
    die.className = `roll-die color-${result.color} face-${result.face}`;
    die.setAttribute("role", "img");
    die.setAttribute("aria-label", `${result.color} ${result.face}`);
    die.style.animationDelay = `${index * 110}ms`;
    const face = document.createElement("img");
    face.src = resultAsset(result);
    face.alt = "";
    die.append(face);
    container.append(die);
  });
}

async function presentRoll(response, automatic) {
  updateGame(response.state);
  renderRoll(response.roll || []);
  const cup = document.getElementById("cup");
  const message = document.getElementById("turn-message");
  cup.classList.remove("lifted");
  cup.classList.remove("shaking");
  cup.classList.toggle("dig-position", automatic);
  cup.classList.toggle("selection-position", !automatic);
  void cup.offsetWidth;
  cup.classList.add("shaking");
  message.textContent = automatic ? "The first dig..." : "Rolling the dice...";
  await delay(640);
  cup.classList.remove("shaking");
  cup.classList.add("lifted");
  await delay(430);
  await delay(900);
  cup.classList.remove("lifted");
  if (automatic) {
    cup.classList.remove("dig-position");
    cup.classList.add("selection-position");
    await delay(420);
  }
  if (response.cavedIn) {
    message.textContent = `CAVE-IN! ${response.gemsLost} gems lost.`;
    await delay(1400);
    await nextTurnAfterEnded(response.state, response.botEvents || []);
    return;
  }
  message.textContent = response.state.canBank
    ? "Dig again, or bank your gems."
    : "No dice remain. Bank your gems.";
  updateGame(response.state);
  if (response.state.canBank) {
    document.getElementById("bank-button").disabled = false;
  }
  document.getElementById("dig-button").disabled = !response.state.canDig;
}

async function nextTurnAfterEnded(state, events) {
  gameState = state;
  if (state.gameOver) {
    await showWinner(state);
    return;
  }
  await delay(500);
  await flickerTransition(() => {
    showScreen("round");
    renderRoundAnnouncement(events);
  });
  await delay(2000);
  await beginTurn(true);
}

document.getElementById("dig-button").addEventListener("click", async () => {
  if (waiting) {
    return;
  }
  waiting = true;
  try {
    const response = await request("/api/action", { action: "dig" });
    await presentRoll(response, false);
  } catch (error) {
    displayError(error);
  } finally {
    waiting = false;
  }
});

document.getElementById("bank-button").addEventListener("click", async () => {
  if (waiting) {
    return;
  }
  waiting = true;
  try {
    const response = await request("/api/action", { action: "bank" });
    updateGame(response.state);
    const message = document.getElementById("turn-message");
    message.textContent = `${response.bankedBy} banked ${response.banked} gems.`;
    await delay(650);
    if (response.winner || response.state.gameOver) {
      await showWinner(response.state);
      return;
    }
    await flickerTransition(() => {
      showScreen("round");
      renderRoundAnnouncement(response.botEvents || []);
    });
    await delay(2000);
    await beginTurn(true);
  } catch (error) {
    displayError(error);
  } finally {
    waiting = false;
  }
});

async function showWinner(state) {
  const winner = state.winner || state.currentPlayer || "A player";
  await flickerTransition(() => showScreen("winner"));
  document.getElementById("winner-intro").hidden = false;
  document.getElementById("winner-name").hidden = true;
  await delay(5000);
  document.getElementById("winner-intro").hidden = true;
  const winnerLabel = document.getElementById("winner-name");
  const player = state.players.find(entry => entry.name === winner);
  winnerLabel.textContent = `${winner} wins with ${player ? player.score : ""} gems!`;
  winnerLabel.hidden = false;
  await delay(10000);
  scene.classList.add("ending");
  await delay(800);
  window.location.reload();
}

function displayError(error) {
  const message = document.getElementById("turn-message");
  message.textContent = error.message;
  message.classList.add("error");
}

async function restoreState() {
  try {
    const response = await fetch("/api/state", { cache: "no-store" });
    const result = await response.json();
    if (result.state && result.state.started) {
      updateGame(result.state);
    }
  } catch (error) {
    console.error("Could not read game state:", error);
  }
}

restoreState();
