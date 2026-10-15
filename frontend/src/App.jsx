import { useEffect, useRef, useState } from "react";
import { Client } from "@stomp/stompjs";
import { apiRequest } from "./api";

const emptyForm = {
    username: "",
    email: "",
    password: ""
};

export default function App() {
    const [user, setUser] = useState(() => readStoredUser());
    const [authMode, setAuthMode] = useState("login");
    const [authForm, setAuthForm] = useState(emptyForm);
    const [error, setError] = useState("");
    const [tickets, setTickets] = useState([]);
    const [agents, setAgents] = useState([]);
    const [selectedTicket, setSelectedTicket] = useState(null);
    const [messages, setMessages] = useState([]);
    const [messageText, setMessageText] = useState("");
    const [typingUsers, setTypingUsers] = useState([]);
    const [ticketForm, setTicketForm] = useState({
        title: "",
        description: "",
        category: "General"
    });
    const [filters, setFilters] = useState({
        status: "",
        priority: "",
        search: ""
    });
    const stompClient = useRef(null);
    const typingTimer = useRef(null);

    function readStoredUser() {
        try {
            return JSON.parse(sessionStorage.getItem("user"));
        } catch {
            return null;
        }
    }

    async function submitAuth(event) {
        event.preventDefault();
        setError("");

        try {
            const endpoint = authMode === "login" ? "/auth/login" : "/auth/register";
            const result = await apiRequest(endpoint, {
                method: "POST",
                body: JSON.stringify(authForm)
            });

            sessionStorage.setItem("token", result.token);
            sessionStorage.setItem("user", JSON.stringify(result));
            setUser(result);
            setAuthForm(emptyForm);
        } catch (exception) {
            setError(exception.message);
        }
    }

    async function loadTickets() {
        if (!user) {
            return;
        }

        try {
            const query = new URLSearchParams({ page: "0", size: "100" });

            if (filters.status) query.set("status", filters.status);
            if (filters.priority) query.set("priority", filters.priority);
            if (filters.search) query.set("search", filters.search);

            const result = await apiRequest(`/tickets?${query.toString()}`);
            setTickets(result.content || []);
        } catch (exception) {
            setError(exception.message);
        }
    }

    async function createTicket(event) {
        event.preventDefault();
        setError("");

        try {
            await apiRequest("/tickets", {
                method: "POST",
                body: JSON.stringify(ticketForm)
            });

            setTicketForm({
                title: "",
                description: "",
                category: "General"
            });

            await loadTickets();
        } catch (exception) {
            setError(exception.message);
        }
    }

    async function openTicket(ticket) {
        setSelectedTicket(ticket);
        setTypingUsers([]);
        setError("");

        try {
            const history = await apiRequest(`/tickets/${ticket.id}/messages`);
            setMessages(history);
            connectToTicket(ticket.id);
        } catch (exception) {
            setError(exception.message);
        }
    }

    function connectToTicket(ticketId) {
        if (stompClient.current) {
            stompClient.current.deactivate();
        }

        const token = sessionStorage.getItem("token");
        const client = new Client({
            brokerURL: "ws://localhost:8080/ws",
            connectHeaders: {
                Authorization: `Bearer ${token}`
            },
            reconnectDelay: 3000,
            onConnect: () => {
                client.subscribe(`/topic/tickets/${ticketId}`, frame => {
                    const message = JSON.parse(frame.body);
                    setMessages(current => {
                        if (current.some(item => item.id === message.id)) return current;
                        return [...current, message];
                    });
                });

                client.subscribe(`/topic/tickets/${ticketId}/typing`, frame => {
                    const event = JSON.parse(frame.body);

                    if (event.userId === user?.id) return;

                    setTypingUsers(event.typing ? [event.username] : []);
                });
            },
            onStompError: () => setError("WebSocket error. Check the backend and ticket access.")
        });

        client.activate();
        stompClient.current = client;
    }

    function sendMessage(event) {
        event.preventDefault();

        if (!selectedTicket || !messageText.trim() || !stompClient.current?.connected) {
            return;
        }

        stompClient.current.publish({
            destination: `/app/tickets/${selectedTicket.id}/message`,
            body: JSON.stringify({ content: messageText.trim() })
        });

        setMessageText("");
    }

    function sendTyping(value) {
        if (!selectedTicket || !stompClient.current?.connected) return;

        stompClient.current.publish({
            destination: `/app/tickets/${selectedTicket.id}/typing`,
            body: JSON.stringify({ typing: value })
        });

        if (typingTimer.current) clearTimeout(typingTimer.current);

        if (value) {
            typingTimer.current = setTimeout(() => sendTyping(false), 1200);
        }
    }

    async function updateStatus(status) {
        if (!selectedTicket) return;

        try {
            const updated = await apiRequest(`/tickets/${selectedTicket.id}/status`, {
                method: "PATCH",
                body: JSON.stringify({ status })
            });

            setSelectedTicket(updated);
            await loadTickets();
        } catch (exception) {
            setError(exception.message);
        }
    }

    async function loadAgents() {
        if (user?.role !== "ADMIN") return;

        try {
            const result = await apiRequest("/admin/agents");
            setAgents(result || []);
        } catch (exception) {
            setError(exception.message);
        }
    }

    async function assignAgent(agentId) {
        if (!selectedTicket || user?.role !== "ADMIN" || !agentId) return;

        try {
            const updated = await apiRequest(`/tickets/${selectedTicket.id}/assign`, {
                method: "POST",
                body: JSON.stringify({ agentId })
            });
            setSelectedTicket(updated);
            await loadTickets();
        } catch (exception) {
            setError(exception.message);
        }
    }

    async function updatePriority(priority) {
        if (!selectedTicket || user?.role !== "ADMIN") return;

        try {
            const updated = await apiRequest(`/tickets/${selectedTicket.id}/priority`, {
                method: "PATCH",
                body: JSON.stringify({ priority })
            });
            setSelectedTicket(updated);
            await loadTickets();
        } catch (exception) {
            setError(exception.message);
        }
    }

    function logout() {
        stompClient.current?.deactivate();
        sessionStorage.removeItem("token");
        sessionStorage.removeItem("user");
        setUser(null);
        setTickets([]);
        setSelectedTicket(null);
        setMessages([]);
    }

    useEffect(() => {
        if (user) loadTickets();
        if (user?.role === "ADMIN") loadAgents();
    }, [user, filters.status, filters.priority, filters.search]);

    useEffect(() => {
        return () => {
            stompClient.current?.deactivate();
            if (typingTimer.current) clearTimeout(typingTimer.current);
        };
    }, []);

    if (!user || !sessionStorage.getItem("token")) {
        return (
            <main className="auth-page">
                <section className="auth-card">
                    <div className="brand-mark">SD</div>
                    <p className="eyebrow">REAL-TIME SUPPORT DESK</p>
                    <h1>{authMode === "login" ? "Welcome back" : "Create your account"}</h1>
                    <p className="muted">Sign in to manage your support requests.</p>

                    <form onSubmit={submitAuth}>
                        {authMode === "register" && (
                            <label>
                                Username
                                <input
                                    required
                                    minLength="3"
                                    value={authForm.username}
                                    onChange={event => setAuthForm({
                                        ...authForm,
                                        username: event.target.value
                                    })}
                                />
                            </label>
                        )}

                        <label>
                            Email address
                            <input
                                type="email"
                                required
                                value={authForm.email}
                                onChange={event => setAuthForm({
                                    ...authForm,
                                    email: event.target.value
                                })}
                            />
                        </label>

                        <label>
                            Password
                            <input
                                type="password"
                                required
                                minLength="8"
                                value={authForm.password}
                                onChange={event => setAuthForm({
                                    ...authForm,
                                    password: event.target.value
                                })}
                            />
                        </label>

                        {error && <p className="error">{error}</p>}
                        <button className="primary full-width">
                            {authMode === "login" ? "Sign in" : "Create account"}
                        </button>
                    </form>

                    <p className="switch-auth">
                        {authMode === "login" ? "New to Support Desk?" : "Already have an account?"}
                        <button
                            className="text-button"
                            onClick={() => {
                                setError("");
                                setAuthMode(authMode === "login" ? "register" : "login");
                            }}
                        >
                            {authMode === "login" ? "Create account" : "Sign in"}
                        </button>
                    </p>
                </section>
            </main>
        );
    }

    return (
        <div className="app-shell">
            <aside className="sidebar">
                <div className="brand-row">
                    <div className="brand-mark">SD</div>
                    <div>
                        <strong>Support Desk</strong>
                        <small>Help center workspace</small>
                    </div>
                </div>

                <div className="profile-card">
                    <div className="avatar">{user.username?.slice(0, 1)?.toUpperCase()}</div>
                    <div>
                        <strong>{user.username}</strong>
                        <small>{user.role?.replace("_", " ")}</small>
                    </div>
                </div>

                <button className="secondary full-width" onClick={logout}>Sign out</button>
                <div className="sidebar-note">
                    <span className="status-dot" />
                    Connected to local workspace
                </div>
            </aside>

            <main className="workspace">
                <header className="page-header">
                    <div>
                        <p className="eyebrow">OVERVIEW</p>
                        <h1>Support tickets</h1>
                        <p className="muted">Track requests and talk with your support team.</p>
                    </div>
                    <span className="count-pill">{tickets.length} tickets</span>
                </header>

                {error && <p className="error page-error">{error}</p>}

                <div className="dashboard-grid">
                    <section className="panel ticket-panel">
                        <div className="panel-heading">
                            <div>
                                <h2>{user.role === "USER" ? "Your tickets" : "Ticket queue"}</h2>
                                <p className="muted">Choose a ticket to open its conversation.</p>
                            </div>
                            <button className="icon-button" onClick={loadTickets} title="Refresh tickets">↻</button>
                        </div>

                        <div className="filter-row">
                            <input
                                placeholder="Search tickets..."
                                value={filters.search}
                                onChange={event => setFilters({ ...filters, search: event.target.value })}
                            />
                            <select
                                value={filters.status}
                                onChange={event => setFilters({ ...filters, status: event.target.value })}
                            >
                                <option value="">All statuses</option>
                                <option value="OPEN">Open</option>
                                <option value="IN_PROGRESS">In progress</option>
                                <option value="RESOLVED">Resolved</option>
                                <option value="CLOSED">Closed</option>
                            </select>
                            <select
                                value={filters.priority}
                                onChange={event => setFilters({ ...filters, priority: event.target.value })}
                            >
                                <option value="">All priorities</option>
                                <option value="LOW">Low</option>
                                <option value="MEDIUM">Medium</option>
                                <option value="HIGH">High</option>
                            </select>
                        </div>

                        <div className="ticket-list">
                            {tickets.length === 0 && (
                                <div className="empty-state">
                                    <div className="empty-icon">□</div>
                                    <strong>No tickets found</strong>
                                    <p>Try changing the filters or create a new ticket.</p>
                                </div>
                            )}

                            {tickets.map(ticket => (
                                <button
                                    key={ticket.id}
                                    className={`ticket-item ${selectedTicket?.id === ticket.id ? "selected" : ""}`}
                                    onClick={() => openTicket(ticket)}
                                >
                                    <div className="ticket-topline">
                                        <strong>{ticket.title}</strong>
                                        <span className={`status status-${ticket.status.toLowerCase()}`}>
                                            {ticket.status.replace("_", " ")}
                                        </span>
                                    </div>
                                    <p>{ticket.description}</p>
                                    <div className="ticket-meta">
                                        <span>{ticket.category}</span>
                                        <span className={`priority priority-${ticket.priority.toLowerCase()}`}>
                                            {ticket.priority}
                                        </span>
                                    </div>
                                </button>
                            ))}
                        </div>
                    </section>

                    <div className="right-column">
                        {user.role === "USER" && (
                            <section className="panel">
                                <div className="panel-heading">
                                    <div>
                                        <h2>Create a ticket</h2>
                                        <p className="muted">Tell us what you need help with.</p>
                                    </div>
                                </div>

                                <form onSubmit={createTicket}>
                                    <label>
                                        Title
                                        <input
                                            required
                                            maxLength="100"
                                            value={ticketForm.title}
                                            onChange={event => setTicketForm({
                                                ...ticketForm,
                                                title: event.target.value
                                            })}
                                        />
                                    </label>
                                    <label>
                                        Category
                                        <select
                                            value={ticketForm.category}
                                            onChange={event => setTicketForm({
                                                ...ticketForm,
                                                category: event.target.value
                                            })}
                                        >
                                            <option>General</option>
                                            <option>Account</option>
                                            <option>Technical</option>
                                            <option>Billing</option>
                                        </select>
                                    </label>

                                    <label>
                                        Description
                                        <textarea
                                            required
                                            rows="3"
                                            maxLength="2000"
                                            value={ticketForm.description}
                                            onChange={event => setTicketForm({
                                                ...ticketForm,
                                                description: event.target.value
                                            })}
                                        />
                                    </label>
                                    <button className="primary full-width">Submit ticket</button>
                                </form>
                            </section>
                        )}

                        <section className="panel conversation-panel">
                            <div className="panel-heading">
                                <div>
                                    <p className="eyebrow">CONVERSATION</p>
                                    <h2>{selectedTicket ? selectedTicket.title : "Select a ticket"}</h2>
                                </div>
                                {selectedTicket && (
                                    <span className={`status status-${selectedTicket.status.toLowerCase()}`}>
                                        {selectedTicket.status.replace("_", " ")}
                                    </span>
                                )}
                            </div>

                            {!selectedTicket ? (
                                <div className="empty-state compact">
                                    <div className="empty-icon">↗</div>
                                    <p>Select a ticket from the list to view messages.</p>
                                </div>
                            ) : (
                                <>
                                    {user.role === "ADMIN" && (
                                        <div className="admin-ticket-controls">
                                            <h3>Manage ticket</h3>
                                            <label>
                                                Priority
                                                <select
                                                    value={selectedTicket.priority}
                                                    onChange={event => updatePriority(event.target.value)}
                                                >
                                                    <option value="LOW">Low</option>
                                                    <option value="MEDIUM">Medium</option>
                                                    <option value="HIGH">High</option>
                                                </select>
                                            </label>
                                            <label>
                                                Assign support agent
                                                <select
                                                    value={selectedTicket.assignedAgentId || ""}
                                                    onChange={event => assignAgent(event.target.value)}
                                                >
                                                    <option value="">Unassigned</option>
                                                    {agents.map(agent => (
                                                        <option key={agent.id} value={agent.id}>
                                                            {agent.username} ({agent.email})
                                                        </option>
                                                    ))}
                                                </select>
                                            </label>
                                            {agents.length === 0 && (
                                                <p className="muted tiny">No support agents found. Promote a registered account to SUPPORT_AGENT first.</p>
                                            )}
                                        </div>
                                    )}
                                    <div className="message-list">
                                        {messages.map(message => (
                                            <div
                                                key={message.id}
                                                className={`message ${message.senderId === user.id ? "mine" : ""}`}
                                            >
                                                <div className="message-author">{message.senderUsername}</div>
                                                <div className="message-bubble">{message.content}</div>
                                                <time>{new Date(message.timestamp).toLocaleTimeString([], {
                                                    hour: "2-digit",
                                                    minute: "2-digit"
                                                })}</time>
                                            </div>
                                        ))}
                                        {typingUsers.length > 0 && (
                                            <p className="typing-indicator">{typingUsers.join(", ")} typing…</p>
                                        )}
                                    </div>

                                    {user.role !== "USER" && (
                                        <div className="status-actions">
                                            {user.role === "ADMIN" && selectedTicket.status === "CLOSED" && (
                                                <button className="primary small" onClick={() => updateStatus("OPEN")}>
                                                    Reopen ticket
                                                </button>
                                            )}
                                            {selectedTicket.status !== "CLOSED" && (
                                                <>
                                                    <button className="secondary small" onClick={() => updateStatus("IN_PROGRESS")}>
                                                        In progress
                                                    </button>
                                                    <button className="secondary small" onClick={() => updateStatus("RESOLVED")}>
                                                        Resolve
                                                    </button>
                                                    <button className="secondary small" onClick={() => updateStatus("CLOSED")}>
                                                        Close
                                                    </button>
                                                </>
                                            )}
                                        </div>
                                    )}

                                    <form className="message-form" onSubmit={sendMessage}>
                                        <input
                                            placeholder="Write a reply..."
                                            value={messageText}
                                            onChange={event => {
                                                setMessageText(event.target.value);
                                                sendTyping(Boolean(event.target.value.trim()));
                                            }}
                                            disabled={selectedTicket.status === "CLOSED"}
                                        />
                                        <button className="primary" disabled={selectedTicket.status === "CLOSED"}>
                                            Send
                                        </button>
                                    </form>
                                    <p className="muted tiny">Messages are sent in real time while the WebSocket is connected.</p>
                                </>
                            )}
                        </section>
                    </div>
                </div>
            </main>
        </div>
    );
}
