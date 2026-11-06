import { useState, type ReactNode } from "react";

type IconName =
  | "grid"
  | "trophy"
  | "users"
  | "user"
  | "chart"
  | "settings"
  | "plus"
  | "search"
  | "bell"
  | "chevron"
  | "arrow"
  | "calendar"
  | "clock"
  | "close"
  | "bolt";

function Icon({ name, size = 18 }: { name: IconName; size?: number }) {
  const paths: Record<IconName, ReactNode> = {
    grid: <><rect x="3" y="3" width="7" height="7" rx="1" /><rect x="14" y="3" width="7" height="7" rx="1" /><rect x="3" y="14" width="7" height="7" rx="1" /><rect x="14" y="14" width="7" height="7" rx="1" /></>,
    trophy: <><path d="M8 21h8M12 17v4M7 4h10v5a5 5 0 0 1-10 0V4Z" /><path d="M7 6H3v2a4 4 0 0 0 4 4M17 6h4v2a4 4 0 0 1-4 4" /></>,
    users: <><path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" /><circle cx="8.5" cy="7" r="4" /><path d="M17 11a4 4 0 0 0 0-8M23 21v-2a4 4 0 0 0-3-3.65" /></>,
    user: <><circle cx="12" cy="8" r="4" /><path d="M4 21a8 8 0 0 1 16 0" /></>,
    chart: <><path d="M4 19V9M10 19V5M16 19v-7M22 19V2" /></>,
    settings: <><circle cx="12" cy="12" r="3" /><path d="M19.4 15a1.7 1.7 0 0 0 .34 1.88l.06.06-2.83 2.83-.06-.06A1.7 1.7 0 0 0 15 19.4a1.7 1.7 0 0 0-1 .6 1.7 1.7 0 0 0-.4 1.1V21h-4v-.1A1.7 1.7 0 0 0 8.6 19.4a1.7 1.7 0 0 0-1.88.34l-.06.06-2.83-2.83.06-.06A1.7 1.7 0 0 0 4.6 15a1.7 1.7 0 0 0-.6-1 1.7 1.7 0 0 0-1.1-.4H3v-4h.1A1.7 1.7 0 0 0 4.6 8.6a1.7 1.7 0 0 0-.34-1.88l-.06-.06 2.83-2.83.06.06A1.7 1.7 0 0 0 9 4.6a1.7 1.7 0 0 0 1-.6 1.7 1.7 0 0 0 .4-1.1V3h4v.1a1.7 1.7 0 0 0 1 1.5 1.7 1.7 0 0 0 1.88-.34l.06-.06 2.83 2.83-.06.06A1.7 1.7 0 0 0 19.4 9c.15.38.36.72.6 1 .3.3.68.45 1.1.45h.1v4h-.1A1.7 1.7 0 0 0 19.4 15Z" /></>,
    plus: <path d="M12 5v14M5 12h14" />,
    search: <><circle cx="11" cy="11" r="7" /><path d="m20 20-4-4" /></>,
    bell: <><path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9M10 21h4" /></>,
    chevron: <path d="m9 18 6-6-6-6" />,
    arrow: <><path d="M5 12h14M13 6l6 6-6 6" /></>,
    calendar: <><rect x="3" y="5" width="18" height="16" rx="2" /><path d="M16 3v4M8 3v4M3 10h18" /></>,
    clock: <><circle cx="12" cy="12" r="9" /><path d="M12 7v5l3 2" /></>,
    close: <><path d="m6 6 12 12M18 6 6 18" /></>,
    bolt: <path d="m13 2-9 12h8l-1 8 9-12h-8l1-8Z" />,
  };

  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      {paths[name]}
    </svg>
  );
}

const navItems = [
  { label: "Overview", icon: "grid" as IconName },
  { label: "Tournaments", icon: "trophy" as IconName },
  { label: "Teams", icon: "users" as IconName },
  { label: "Players", icon: "user" as IconName },
  { label: "Statistics", icon: "chart" as IconName },
];

const matches = [
  { date: "TODAY", time: "18:00", event: "NOVA Masters", stage: "Upper bracket · Round 2", a: "PHANTOM", b: "KINETIC", score: "—  :  —", live: false, colors: ["violet", "blue"] },
  { date: "TODAY", time: "20:30", event: "NOVA Masters", stage: "Upper bracket · Round 2", a: "VERTEX", b: "EMBER", score: "—  :  —", live: false, colors: ["lime", "orange"] },
  { date: "MAY 21", time: "21:14", event: "Apex Open", stage: "Grand final · Best of 3", a: "PHANTOM", b: "VERTEX", score: "2  :  1", live: false, colors: ["violet", "lime"] },
];

const teams = [
  { rank: "01", name: "PHANTOM", country: "EU", record: "18–4", form: ["W", "W", "W", "L", "W"], color: "violet", delta: "+1" },
  { rank: "02", name: "VERTEX", country: "US", record: "16–5", form: ["W", "L", "W", "W", "W"], color: "lime", delta: "—" },
  { rank: "03", name: "KINETIC", country: "SE", record: "15–6", form: ["L", "W", "W", "W", "L"], color: "blue", delta: "+2" },
  { rank: "04", name: "EMBER", country: "DK", record: "13–7", form: ["W", "W", "L", "L", "W"], color: "orange", delta: "-1" },
];

function TeamMark({ color, small = false }: { color: string; small?: boolean }) {
  return <div className={`team-mark ${color} ${small ? "small" : ""}`}><Icon name="bolt" size={small ? 14 : 20} /></div>;
}

function Sidebar({ active, setActive }: { active: string; setActive: (v: string) => void }) {
  return (
    <aside className="sidebar">
      <div className="brand"><div className="brand-mark">A</div><span>ARENA<span className="brand-dot">.</span></span></div>
      <div className="workspace">
        <span className="eyebrow">WORKSPACE</span>
        <button className="workspace-picker"><span className="workspace-logo">N</span><span><b>NOVA League</b><small>2025 Season</small></span><Icon name="chevron" size={15} /></button>
      </div>
      <nav className="nav">
        <span className="eyebrow">MANAGE</span>
        {navItems.map((item) => (
          <button key={item.label} className={`nav-item ${active === item.label ? "active" : ""}`} onClick={() => setActive(item.label)}>
            <Icon name={item.icon} /><span>{item.label}</span>{item.label === "Tournaments" && <span className="count">4</span>}
          </button>
        ))}
      </nav>
      <div className="sidebar-footer">
        <button className="nav-item"><Icon name="settings" /><span>Settings</span></button>
        <div className="profile"><div className="avatar">OM</div><div><b>Owen Mitchell</b><small>League Admin</small></div><button aria-label="Open profile"><span>•••</span></button></div>
      </div>
    </aside>
  );
}

function Header({ onCreate }: { onCreate: () => void }) {
  return (
    <header className="topbar">
      <div className="search"><Icon name="search" /><input placeholder="Search teams, players, tournaments..." /><kbd>⌘ K</kbd></div>
      <div className="top-actions"><button className="icon-button" aria-label="Notifications"><Icon name="bell" /><span className="notification" /></button><button className="primary" onClick={onCreate}><Icon name="plus" size={16} />Create new</button></div>
    </header>
  );
}

function MatchRow({ match, onOpen }: { match: typeof matches[number]; onOpen: () => void }) {
  return (
    <button className="match-row" onClick={onOpen}>
      <div className="match-time"><span>{match.date}</span><b>{match.time}</b></div>
      <div className="event-info"><b>{match.event}</b><span>{match.stage}</span></div>
      <div className="match-teams">
        <div><b>{match.a}</b><TeamMark color={match.colors[0]} small /></div>
        <strong>{match.score}</strong>
        <div><TeamMark color={match.colors[1]} small /><b>{match.b}</b></div>
      </div>
      <span className="row-arrow"><Icon name="chevron" size={16} /></span>
    </button>
  );
}

function Overview({ openMatch, onCreate }: { openMatch: () => void; onCreate: () => void }) {
  return (
    <main className="content">
      <div className="page-heading">
        <div><span className="eyebrow accent">COMPETITION CONTROL</span><h1>Good morning, Owen<span>.</span></h1><p>Here’s what’s happening across your competitions.</p></div>
        <div className="date-chip"><Icon name="calendar" size={16} />May 24, 2025</div>
      </div>

      <section className="stats-grid">
        <div className="stat-card spotlight"><span className="eyebrow">ACTIVE TOURNAMENTS</span><strong>04</strong><div className="stat-footer"><span className="status-dot" />2 matches live today</div><div className="watermark"><Icon name="trophy" size={90} /></div></div>
        <div className="stat-card"><span className="eyebrow">REGISTERED TEAMS</span><strong>28</strong><div className="stat-footer positive">↑ 4 <span>this month</span></div></div>
        <div className="stat-card"><span className="eyebrow">TOTAL PLAYERS</span><strong>164</strong><div className="stat-footer positive">↑ 12 <span>this month</span></div></div>
        <div className="stat-card"><span className="eyebrow">MATCHES PLAYED</span><strong>96</strong><div className="stat-footer muted">18 scheduled next</div></div>
      </section>

      <div className="dashboard-grid">
        <section className="panel schedule-panel">
          <div className="section-title"><div><span className="eyebrow">SCHEDULE</span><h2>Upcoming matches</h2></div><button className="text-button">View all <Icon name="arrow" size={15} /></button></div>
          <div className="live-banner">
            <div className="live-copy"><span className="live-tag"><i />LIVE</span><b>NOVA MASTERS 2025</b><small>Lower bracket · Round 1</small></div>
            <div className="live-teams"><div><TeamMark color="orange" /><b>EMBER</b></div><strong><span>8</span><i>:</i><span>11</span><small>MAP 1 · ANCIENT</small></strong><div><b>KINETIC</b><TeamMark color="blue" /></div></div>
            <button className="watch" onClick={openMatch}>Watch match <Icon name="arrow" size={15} /></button>
          </div>
          <div className="match-list">{matches.map((match, index) => <MatchRow key={`${match.a}-${match.b}`} match={match} onOpen={index === 0 ? openMatch : () => {}} />)}</div>
        </section>

        <aside className="right-column">
          <section className="panel ranking-panel">
            <div className="section-title"><div><span className="eyebrow">POWER RANKING</span><h2>Top teams</h2></div><button className="icon-button small"><Icon name="arrow" size={15} /></button></div>
            <div className="ranking-list">
              {teams.map((team) => <div className="rank-row" key={team.name}><span className="rank">{team.rank}</span><TeamMark color={team.color} small /><div className="rank-name"><b>{team.name}</b><small>{team.country} · {team.record}</small></div><div className="form">{team.form.map((v, i) => <i key={i} className={v === "W" ? "win" : "loss"} />)}</div><span className={`delta ${team.delta.startsWith("+") ? "up" : ""}`}>{team.delta}</span></div>)}
            </div>
          </section>
          <section className="promo">
            <span className="eyebrow">QUICK ACTION</span><h2>Ready to build your next competition?</h2><p>Create brackets, invite teams, and go live in minutes.</p><button onClick={onCreate}>Create tournament <Icon name="arrow" size={16} /></button>
            <div className="promo-lines" />
          </section>
        </aside>
      </div>
    </main>
  );
}

function MatchPage({ onBack }: { onBack: () => void }) {
  const [tab, setTab] = useState("Overview");
  return (
    <main className="content match-page">
      <button className="back-button" onClick={onBack}>← Back to overview</button>
      <div className="match-hero">
        <div className="match-event-label"><span className="live-tag"><i />LIVE</span><span>NOVA MASTERS 2025</span><span>·</span><span>LOWER BRACKET — ROUND 1</span></div>
        <div className="versus">
          <div className="hero-team"><TeamMark color="orange" /><h1>EMBER</h1><span>Denmark</span></div>
          <div className="hero-score"><div><strong>8</strong><i>:</i><strong>11</strong></div><span>MAP 1 · BEST OF 3</span><small>01:12:44</small></div>
          <div className="hero-team"><TeamMark color="blue" /><h1>KINETIC</h1><span>Sweden</span></div>
        </div>
        <div className="series-score"><span>EMBER</span><div><i className="active" /><i /><i /></div><b>0 — 0</b><div><i className="active blue" /><i /><i /></div><span>KINETIC</span></div>
      </div>
      <div className="tabs">{["Overview", "Scoreboard", "Economy", "Head to head"].map((item) => <button key={item} className={tab === item ? "active" : ""} onClick={() => setTab(item)}>{item}</button>)}</div>
      <div className="match-details-grid">
        <section className="panel map-panel">
          <div className="section-title"><div><span className="eyebrow">CURRENT MAP</span><h2>Ancient</h2></div><span className="round-badge">Round 20 / 24</span></div>
          <div className="round-history"><span>EMBER</span><div>{Array.from({ length: 19 }).map((_, i) => <i key={i} className={i < 8 ? "orange-round" : i < 11 ? "blue-round" : i % 2 ? "blue-round" : "orange-round"} />)}</div><span>KINETIC</span></div>
          <div className="map-stats">
            <div><span>First kills</span><b>10</b><strong>9</strong></div>
            <div><span>Clutches</span><b>2</b><strong>3</strong></div>
            <div><span>Utility damage</span><b>412</b><strong>536</strong></div>
            <div><span>Team rating</span><b>0.94</b><strong>1.12</strong></div>
          </div>
        </section>
        <section className="panel players-panel">
          <div className="section-title"><div><span className="eyebrow">MATCH LEADERS</span><h2>Top performers</h2></div></div>
          {[
            ["K", "kinzo", "KINETIC", "22–12", "1.42"],
            ["V", "vex", "EMBER", "19–14", "1.21"],
            ["S", "soren", "KINETIC", "18–13", "1.18"],
          ].map((p, i) => <div className="player-row" key={p[1]}><span className="player-rank">0{i + 1}</span><div className="player-avatar">{p[0]}</div><div><b>{p[1]}</b><small>{p[2]}</small></div><span>{p[3]} K–D</span><strong>{p[4]}</strong></div>)}
        </section>
      </div>
    </main>
  );
}

const bracketRounds = [
  {
    title: "Quarterfinals",
    date: "MAY 20",
    matches: [
      { a: "PHANTOM", b: "EMBER", as: 2, bs: 0, colors: ["violet", "orange"], done: true },
      { a: "VERTEX", b: "NORTH", as: 2, bs: 1, colors: ["lime", "blue"], done: true },
      { a: "KINETIC", b: "ATLAS", as: 2, bs: 0, colors: ["blue", "orange"], done: true },
      { a: "RAVEN", b: "ECHO", as: 1, bs: 2, colors: ["violet", "lime"], done: true },
    ],
  },
  {
    title: "Semifinals",
    date: "MAY 24",
    matches: [
      { a: "PHANTOM", b: "VERTEX", as: 1, bs: 0, colors: ["violet", "lime"], done: false },
      { a: "KINETIC", b: "ECHO", as: 0, bs: 0, colors: ["blue", "lime"], done: false },
    ],
  },
  {
    title: "Grand final",
    date: "JUN 02",
    matches: [
      { a: "TBD", b: "TBD", as: 0, bs: 0, colors: ["muted", "muted"], done: false },
    ],
  },
];

function BracketMatch({ match, featured = false }: { match: typeof bracketRounds[number]["matches"][number]; featured?: boolean }) {
  return (
    <button className={`bracket-match ${featured ? "featured" : ""}`}>
      <div className={match.as > match.bs ? "winner" : ""}><TeamMark color={match.colors[0]} small /><b>{match.a}</b><strong>{match.done || match.as ? match.as : "—"}</strong></div>
      <div className={match.bs > match.as ? "winner" : ""}><TeamMark color={match.colors[1]} small /><b>{match.b}</b><strong>{match.done || match.bs ? match.bs : "—"}</strong></div>
      {featured && <span className="match-live-label"><i />LIVE · MAP 2</span>}
    </button>
  );
}

function TournamentDetail({ onBack, onOpenMatch }: { onBack: () => void; onOpenMatch: () => void }) {
  const [section, setSection] = useState("Bracket");
  const [bracket, setBracket] = useState("Playoffs");
  return (
    <main className="content tournament-detail">
      <button className="back-button" onClick={onBack}>← All tournaments</button>
      <section className="tournament-hero">
        <div className="tournament-emblem"><Icon name="trophy" size={34} /></div>
        <div className="tournament-identity">
          <div><span className="live-tag"><i />LIVE</span><span className="tournament-tier">S-TIER</span></div>
          <h1>NOVA MASTERS 2025</h1>
          <p>Europe · Online & Berlin, Germany</p>
        </div>
        <div className="tournament-meta">
          <div><span>DATES</span><b>May 18 — Jun 02</b></div>
          <div><span>PRIZE POOL</span><b>$100,000 USD</b></div>
          <div><span>TEAMS</span><b>16</b></div>
        </div>
        <button className="secondary manage-button">Manage tournament</button>
      </section>

      <div className="tournament-nav">
        {["Overview", "Matches", "Bracket", "Teams", "Statistics"].map((item) => <button key={item} className={section === item ? "active" : ""} onClick={() => setSection(item)}>{item}{item === "Matches" && <span>12</span>}</button>)}
      </div>

      <div className="tournament-content-grid">
        <section className="panel bracket-panel">
          <div className="bracket-toolbar">
            <div><span className="eyebrow">DOUBLE ELIMINATION</span><h2>Tournament bracket</h2></div>
            <div className="bracket-switch">{["Groups", "Playoffs"].map((item) => <button key={item} className={bracket === item ? "active" : ""} onClick={() => setBracket(item)}>{item}</button>)}</div>
          </div>
          <div className="bracket-section-label"><span>UPPER BRACKET</span><i /></div>
          <div className="bracket">
            {bracketRounds.map((round, roundIndex) => (
              <div className={`bracket-round round-${roundIndex + 1}`} key={round.title}>
                <div className="round-title"><b>{round.title}</b><span>{round.date}</span></div>
                <div className="round-matches">
                  {round.matches.map((match, index) => <BracketMatch key={`${match.a}-${match.b}`} match={match} featured={roundIndex === 1 && index === 0} />)}
                </div>
              </div>
            ))}
          </div>
          <div className="bracket-section-label lower"><span>LOWER BRACKET</span><i /></div>
          <div className="lower-bracket">
            <div>
              <span>LOWER ROUND 1</span>
              <BracketMatch match={{ a: "EMBER", b: "NORTH", as: 1, bs: 2, colors: ["orange", "blue"], done: true }} />
              <BracketMatch match={{ a: "ATLAS", b: "RAVEN", as: 0, bs: 2, colors: ["orange", "violet"], done: true }} />
            </div>
            <div>
              <span>LOWER QUARTERFINAL</span>
              <BracketMatch match={{ a: "NORTH", b: "ECHO", as: 0, bs: 0, colors: ["blue", "lime"], done: false }} />
              <BracketMatch match={{ a: "RAVEN", b: "TBD", as: 0, bs: 0, colors: ["violet", "muted"], done: false }} />
            </div>
            <div>
              <span>LOWER SEMIFINAL</span>
              <BracketMatch match={{ a: "TBD", b: "TBD", as: 0, bs: 0, colors: ["muted", "muted"], done: false }} />
            </div>
          </div>
        </section>

        <aside className="tournament-aside">
          <section className="panel next-match-card">
            <div className="section-title"><div><span className="eyebrow">ON AIR NOW</span><h2>Featured match</h2></div><span className="live-tag"><i />LIVE</span></div>
            <div className="featured-versus">
              <div><TeamMark color="violet" /><b>PHANTOM</b><span>#1</span></div>
              <strong><span>1</span><i>:</i><span>0</span><small>BO3 · MAP 2</small></strong>
              <div><TeamMark color="lime" /><b>VERTEX</b><span>#2</span></div>
            </div>
            <button className="primary" onClick={onOpenMatch}>Open match center <Icon name="arrow" size={15} /></button>
          </section>
          <section className="panel standings-card">
            <div className="section-title"><div><span className="eyebrow">GROUP STAGE</span><h2>Final standings</h2></div><button className="icon-button small"><Icon name="arrow" size={15} /></button></div>
            {teams.map((team, index) => <div className="standing-row" key={team.name}><span>0{index + 1}</span><TeamMark color={team.color} small /><b>{team.name}</b><small>{index < 2 ? "Upper bracket" : "Lower bracket"}</small><strong>{4 - index}–{index}</strong></div>)}
          </section>
          <section className="panel prize-card">
            <div><span className="eyebrow">PRIZE DISTRIBUTION</span><h2>$100,000</h2><p>Total tournament purse</p></div>
            <div className="prize-bars"><i /><i /><i /><i /></div>
            <div className="prize-legend"><span><i />1st <b>$50k</b></span><span><i />2nd <b>$25k</b></span><span><i />3–4th <b>$12.5k</b></span></div>
          </section>
        </aside>
      </div>
    </main>
  );
}

type RosterPlayer = {
  id: number;
  nick: string;
  name: string;
  country: string;
  role: string;
  rating: string;
  avatar: string;
  status: string;
};

type RosterEvent = {
  id: number;
  player: string;
  name: string;
  avatar: string;
  action: "Joined" | "Benched" | "Departed";
  detail: string;
  date: string;
};

const initialRoster: RosterPlayer[] = [
  { id: 1, nick: "flux", name: "Luka Varga", country: "HU", role: "In-game leader", rating: "1.12", avatar: "FV", status: "Starter" },
  { id: 2, nick: "m0nt", name: "Milan Kovac", country: "RS", role: "AWPer", rating: "1.24", avatar: "MK", status: "Starter" },
  { id: 3, nick: "raye", name: "Elias Berg", country: "SE", role: "Entry fragger", rating: "1.18", avatar: "EB", status: "Starter" },
  { id: 4, nick: "nox", name: "Noah Weber", country: "DE", role: "Rifler", rating: "1.09", avatar: "NW", status: "Starter" },
  { id: 5, nick: "kairo", name: "Kai Rhodes", country: "GB", role: "Support", rating: "1.07", avatar: "KR", status: "Starter" },
  { id: 6, nick: "orbit", name: "Oskar Lind", country: "SE", role: "Substitute", rating: "1.04", avatar: "OL", status: "Bench" },
];

const initialRosterHistory: RosterEvent[] = [
  { id: 1, player: "orbit", name: "Oskar Lind", avatar: "OL", action: "Benched", detail: "Moved from starting lineup", date: "May 06, 2025" },
  { id: 2, player: "kairo", name: "Kai Rhodes", avatar: "KR", action: "Joined", detail: "Transferred from NORTH", date: "Feb 18, 2025" },
  { id: 3, player: "sylo", name: "Marek Nowak", avatar: "MN", action: "Departed", detail: "Transferred to VERTEX", date: "Feb 16, 2025" },
  { id: 4, player: "nox", name: "Noah Weber", avatar: "NW", action: "Joined", detail: "Signed as free agent", date: "Oct 11, 2024" },
  { id: 5, player: "drift", name: "Ivan Petrov", avatar: "IP", action: "Departed", detail: "Contract expired", date: "Sep 30, 2024" },
];

const availablePlayers: RosterPlayer[] = [
  { id: 11, nick: "zeph", name: "Mateo Silva", country: "ES", role: "Rifler", rating: "1.21", avatar: "MS", status: "Free agent" },
  { id: 12, nick: "clutch", name: "Jonas Meyer", country: "DE", role: "Support", rating: "1.16", avatar: "JM", status: "Free agent" },
  { id: 13, nick: "sway", name: "Alex Novak", country: "CZ", role: "Entry fragger", rating: "1.14", avatar: "AN", status: "Free agent" },
  { id: 14, nick: "frost", name: "Nils Eriksen", country: "NO", role: "AWPer", rating: "1.11", avatar: "NE", status: "KINETIC" },
];

function AssignPlayerModal({ roster, onAssign, onClose }: { roster: RosterPlayer[]; onAssign: (player: RosterPlayer) => void; onClose: () => void }) {
  const [query, setQuery] = useState("");
  const [selected, setSelected] = useState<RosterPlayer | null>(null);
  const [role, setRole] = useState("Substitute");
  const filtered = availablePlayers.filter((player) => !roster.some((member) => member.id === player.id) && `${player.nick} ${player.name}`.toLowerCase().includes(query.toLowerCase()));
  return (
    <div className="modal-backdrop" onMouseDown={(event) => event.target === event.currentTarget && onClose()}>
      <div className="modal assign-modal">
        <div className="modal-head"><div><span className="eyebrow">PHANTOM ROSTER</span><h2>Assign player</h2></div><button className="icon-button" onClick={onClose}><Icon name="close" /></button></div>
        <div className="assign-search"><Icon name="search" size={16} /><input autoFocus value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search player database..." /></div>
        <div className="available-player-list">
          {filtered.map((player) => <button key={player.id} className={selected?.id === player.id ? "selected" : ""} onClick={() => setSelected(player)}>
            <div className="roster-avatar">{player.avatar}</div>
            <div><b>{player.nick}</b><span>{player.name} · {player.country}</span></div>
            <small>{player.role}</small><strong>{player.rating}</strong>
          </button>)}
        </div>
        <div className="assignment-role">
          <label><span>Roster position</span><select value={role} onChange={(event) => setRole(event.target.value)}><option>Starter</option><option>Substitute</option></select></label>
          <div><span className="eyebrow">SELECTED PLAYER</span><b>{selected ? selected.nick : "None"}</b></div>
        </div>
        <div className="modal-actions"><button className="secondary" onClick={onClose}>Cancel</button><button className="primary" disabled={!selected} onClick={() => selected && onAssign({ ...selected, status: role })}>Assign to PHANTOM <Icon name="plus" size={15} /></button></div>
      </div>
    </div>
  );
}

function TeamDetail({ onBack }: { onBack: () => void }) {
  const [roster, setRoster] = useState(initialRoster);
  const [rosterHistory, setRosterHistory] = useState(initialRosterHistory);
  const [assigning, setAssigning] = useState(false);
  const removePlayer = (id: number) => {
    const player = roster.find((member) => member.id === id);
    if (!player) return;
    setRoster((players) => players.filter((member) => member.id !== id));
    setRosterHistory((events) => [{
      id: Date.now(),
      player: player.nick,
      name: player.name,
      avatar: player.avatar,
      action: "Departed",
      detail: "Removed from active roster",
      date: "May 24, 2025",
    }, ...events]);
  };
  const assignPlayer = (player: RosterPlayer) => {
    setRoster((players) => [...players, player]);
    setRosterHistory((events) => [{
      id: Date.now(),
      player: player.nick,
      name: player.name,
      avatar: player.avatar,
      action: "Joined",
      detail: player.status === "Starter" ? "Added to starting lineup" : "Added as substitute",
      date: "May 24, 2025",
    }, ...events]);
    setAssigning(false);
  };
  const starters = roster.filter((player) => player.status === "Starter");
  const bench = roster.filter((player) => player.status !== "Starter");
  return (
    <main className="content team-detail">
      <button className="back-button" onClick={onBack}>← All teams</button>
      <section className="team-hero">
        <TeamMark color="violet" />
        <div className="team-identity"><span className="eyebrow">EUROPE · RANK #1</span><h1>PHANTOM</h1><p>Active since 2022 · Berlin, Germany</p></div>
        <div className="team-record"><div><span>WORLD RANK</span><b>#01</b></div><div><span>RECORD</span><b>18–4</b></div><div><span>WIN RATE</span><b>81.8%</b></div><div><span>ROSTER</span><b>{roster.length}/8</b></div></div>
        <button className="primary" onClick={() => setAssigning(true)}><Icon name="plus" size={16} />Assign player</button>
      </section>

      <div className="team-detail-grid">
        <div className="team-main-column">
          <section className="panel roster-panel">
            <div className="section-title"><div><span className="eyebrow team-roster-eyebrow">ACTIVE LINEUP</span><h2>Team roster</h2></div><div className="roster-count"><span>{starters.length}</span> starters · <span>{bench.length}</span> bench</div></div>
            <div className="roster-table-head"><span>PLAYER</span><span>ROLE</span><span>RATING 3M</span><span>STATUS</span><span /></div>
            <div className="roster-group-label"><span>STARTING FIVE</span><i /></div>
            {starters.map((player) => <div className="roster-row" key={player.id}>
              <div><div className="roster-avatar">{player.avatar}</div><span><b>{player.nick}</b><small>{player.name} · {player.country}</small></span></div>
              <span>{player.role}</span><strong>{player.rating}</strong><span className="roster-status active"><i />Starter</span>
              <button className="roster-menu" onClick={() => removePlayer(player.id)} title="Remove from roster">×</button>
            </div>)}
            <div className="roster-group-label"><span>BENCH & SUBSTITUTES</span><i /></div>
            {bench.map((player) => <div className="roster-row benched" key={player.id}>
              <div><div className="roster-avatar">{player.avatar}</div><span><b>{player.nick}</b><small>{player.name} · {player.country}</small></span></div>
              <span>{player.role}</span><strong>{player.rating}</strong><span className="roster-status"><i />{player.status}</span>
              <button className="roster-menu" onClick={() => removePlayer(player.id)} title="Remove from roster">×</button>
            </div>)}
            <button className="empty-roster-slot" onClick={() => setAssigning(true)}><span><Icon name="plus" size={16} /></span><div><b>Add another player</b><small>{Math.max(0, 8 - roster.length)} roster slots available</small></div></button>
          </section>

          <section className="panel roster-history-panel">
            <div className="section-title"><div><span className="eyebrow">TEAM CHANGES</span><h2>Roster history</h2></div><div className="history-total">{rosterHistory.length} recorded moves</div></div>
            <div className="history-table-head"><span>PLAYER</span><span>CHANGE</span><span>DETAILS</span><span>DATE</span></div>
            <div className="history-list">
              {rosterHistory.map((event) => <div className="history-row" key={event.id}>
                <div><div className="roster-avatar">{event.avatar}</div><span><b>{event.player}</b><small>{event.name}</small></span></div>
                <span className={`history-action ${event.action.toLowerCase()}`}><i />{event.action}</span>
                <span className="history-detail">{event.detail}</span>
                <time>{event.date}</time>
              </div>)}
            </div>
          </section>
        </div>

        <aside className="team-aside">
          <section className="panel staff-card">
            <div className="section-title"><div><span className="eyebrow">TEAM STAFF</span><h2>Coach</h2></div><button className="icon-button small"><Icon name="settings" size={14} /></button></div>
            <div className="coach-profile"><div className="coach-avatar">AM</div><div><b>Andrej “marshal” Markovic</b><span>Head coach · RS</span><small>Joined Aug 2023</small></div></div>
          </section>
          <section className="panel role-health">
            <div className="section-title"><div><span className="eyebrow">LINEUP CHECK</span><h2>Role coverage</h2></div><span className="complete-pill">COMPLETE</span></div>
            {["In-game leader", "AWPer", "Entry fragger", "Rifler", "Support"].map((role) => <div className="role-row" key={role}><span><i />{role}</span><b>{starters.find((player) => player.role === role)?.nick ?? "Missing"}</b></div>)}
          </section>
          <section className="panel roster-note"><Icon name="users" size={20} /><div><b>Tournament eligibility</b><p>This roster meets the NOVA Masters requirement of five starters and one registered substitute.</p></div></section>
        </aside>
      </div>
      {assigning && <AssignPlayerModal roster={roster} onAssign={assignPlayer} onClose={() => setAssigning(false)} />}
    </main>
  );
}

function DirectoryPage({ type, onCreate, onOpenTournament, onOpenTeam }: { type: string; onCreate: () => void; onOpenTournament?: () => void; onOpenTeam?: () => void }) {
  const isTournament = type === "Tournaments";
  return (
    <main className="content">
      <div className="page-heading"><div><span className="eyebrow accent">NOVA LEAGUE DATABASE</span><h1>{type}<span>.</span></h1><p>Manage every {type.toLowerCase().slice(0, -1)} in your competition network.</p></div><button className="primary large" onClick={onCreate}><Icon name="plus" size={16} />Add {type.slice(0, -1).toLowerCase()}</button></div>
      <section className="panel directory-panel">
        <div className="directory-toolbar"><div className="search compact"><Icon name="search" /><input placeholder={`Search ${type.toLowerCase()}...`} /></div><div className="filters"><button>All regions <span>⌄</span></button><button>All status <span>⌄</span></button></div></div>
        {isTournament ? (
          <div className="tournament-grid">{[
            ["NOVA MASTERS 2025", "LIVE", "16 teams", "May 18 — Jun 02", "violet"],
            ["APEX OPEN", "UPCOMING", "24 teams", "Jun 12 — Jun 28", "blue"],
            ["MIDNIGHT CUP", "REGISTRATION", "8 teams", "Jul 04 — Jul 06", "orange"],
            ["NOVA CHALLENGERS", "COMPLETED", "32 teams", "Apr 02 — Apr 21", "lime"],
          ].map((t) => <article className="tournament-card" key={t[0]}><div className={`tournament-art ${t[4]}`}><Icon name="trophy" size={34} /><span>{t[1]}</span></div><div><h2>{t[0]}</h2><p>{t[2]} · {t[3]}</p><button onClick={onOpenTournament}>Open tournament <Icon name="arrow" size={14} /></button></div></article>)}</div>
        ) : (
          <div className="table">
            <div className="table-head"><span>RANK / NAME</span><span>REGION</span><span>{type === "Players" ? "TEAM" : "PLAYERS"}</span><span>RECORD</span><span>STATUS</span><span /></div>
            {(type === "Players" ? [
              ["01", "kinzo", "SE", "KINETIC", "1.31", "Active", "blue"],
              ["02", "vex", "DK", "EMBER", "1.26", "Active", "orange"],
              ["03", "soren", "SE", "KINETIC", "1.22", "Active", "blue"],
              ["04", "flux", "EU", "PHANTOM", "1.19", "Benched", "violet"],
            ] : teams.map((t) => [t.rank, t.name, t.country, "5 / 7", t.record, "Active", t.color])).map((row) => <div className="table-row" key={row[1]}><span><b>{row[0]}</b><TeamMark color={row[6]} small /><strong>{row[1]}</strong></span><span>{row[2]}</span><span>{row[3]}</span><span>{row[4]}</span><span><i className="status-dot" />{row[5]}</span><button onClick={type === "Teams" ? onOpenTeam : undefined}>•••</button></div>)}
          </div>
        )}
      </section>
    </main>
  );
}

function CreateModal({ initial, onClose }: { initial: string; onClose: () => void }) {
  const [type, setType] = useState(initial === "Overview" ? "Tournament" : initial.slice(0, -1));
  const [created, setCreated] = useState(false);
  return (
    <div className="modal-backdrop" onMouseDown={(e) => e.target === e.currentTarget && onClose()}>
      <div className="modal">
        {created ? <div className="success-state"><div className="success-icon">✓</div><h2>{type} created</h2><p>Your new {type.toLowerCase()} is ready to configure.</p><button className="primary" onClick={onClose}>Done</button></div> : <>
          <div className="modal-head"><div><span className="eyebrow">NEW ENTRY</span><h2>Create {type.toLowerCase()}</h2></div><button className="icon-button" onClick={onClose}><Icon name="close" /></button></div>
          <div className="type-switch">{["Tournament", "Team", "Player"].map((t) => <button key={t} className={type === t ? "active" : ""} onClick={() => setType(t)}>{t}</button>)}</div>
          <label><span>{type} name</span><input autoFocus placeholder={type === "Player" ? "Player nickname" : `Enter ${type.toLowerCase()} name`} /></label>
          <div className="form-grid"><label><span>{type === "Tournament" ? "Start date" : "Region"}</span><input type={type === "Tournament" ? "date" : "text"} placeholder="Select region" /></label><label><span>{type === "Tournament" ? "Team limit" : "Status"}</span><select><option>{type === "Tournament" ? "16 teams" : "Active"}</option><option>{type === "Tournament" ? "32 teams" : "Inactive"}</option></select></label></div>
          <label><span>Description <i>Optional</i></span><textarea placeholder="Add a short description..." /></label>
          <div className="modal-actions"><button className="secondary" onClick={onClose}>Cancel</button><button className="primary" onClick={() => setCreated(true)}>Create {type.toLowerCase()}<Icon name="arrow" size={15} /></button></div>
        </>}
      </div>
    </div>
  );
}

export default function App() {
  const [active, setActive] = useState("Overview");
  const [showMatch, setShowMatch] = useState(false);
  const [showTournament, setShowTournament] = useState(false);
  const [showTeam, setShowTeam] = useState(false);
  const [modal, setModal] = useState(false);

  const changePage = (page: string) => { setActive(page); setShowMatch(false); setShowTournament(false); setShowTeam(false); };
  return (
    <div className="app-shell">
      <Sidebar active={active} setActive={changePage} />
      <div className="app-main">
        <Header onCreate={() => setModal(true)} />
        {showMatch ? <MatchPage onBack={() => setShowMatch(false)} /> : showTournament ? <TournamentDetail onBack={() => setShowTournament(false)} onOpenMatch={() => setShowMatch(true)} /> : showTeam ? <TeamDetail onBack={() => setShowTeam(false)} /> : active === "Overview" ? <Overview openMatch={() => setShowMatch(true)} onCreate={() => setModal(true)} /> : ["Tournaments", "Teams", "Players"].includes(active) ? <DirectoryPage type={active} onCreate={() => setModal(true)} onOpenTournament={() => setShowTournament(true)} onOpenTeam={() => setShowTeam(true)} /> : <DirectoryPage type="Players" onCreate={() => setModal(true)} />}
      </div>
      {modal && <CreateModal initial={active} onClose={() => setModal(false)} />}
    </div>
  );
}
