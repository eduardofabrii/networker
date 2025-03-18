// Variáveis globais
let stompClient = null;
let currentUsername = null;
let usernameModal = null;

// Elementos do DOM
const messageLogElement = document.getElementById('messageLog');
const connectedUsersElement = document.getElementById('connectedUsers');

// Inicializar a aplicação
document.addEventListener('DOMContentLoaded', function() {
    // Inicializar componentes Bootstrap
    usernameModal = new bootstrap.Modal(document.getElementById('usernameModal'));
    
    // Configurar eventos
    document.getElementById('sendChatBtn').addEventListener('click', sendChatMessage);
    document.getElementById('chatMessage').addEventListener('keypress', function(event) {
        if (event.key === 'Enter') {
            sendChatMessage();
        }
    });
    document.getElementById('usernameSubmit').addEventListener('click', setUsername);
    
    // Mostrar modal de login ao carregar a página
    showUsernameModal();

    // Toggle do sidebar para mobile
    document.getElementById('mobileToggle')?.addEventListener('click', function() {
        document.querySelector('.sidebar').classList.toggle('show');
    });
});

// Mostrar modal de nome de usuário
function showUsernameModal() {
    // Gerar nome aleatório
    const randomUsername = 'usuario-' + Math.floor(Math.random() * 9000 + 1000);
    document.getElementById('username').value = randomUsername;
    usernameModal.show();
}

// Definir nome do usuário
function setUsername() {
    const username = document.getElementById('username').value.trim();
    
    if (username) {
        currentUsername = username;
        usernameModal.hide();
        init();
        registerUser(username);
    } else {
        document.getElementById('username').classList.add('is-invalid');
        setTimeout(() => document.getElementById('username').classList.remove('is-invalid'), 2000);
    }
}

// Registrar usuário no servidor
function registerUser(username) {
    fetch('/api/tcp/users/register', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({ username: username })
    })
    .then(response => {
        if (!response.ok) throw new Error(`Erro HTTP: ${response.status}`);
        return response.json();
    })
    .then(data => {
        setTimeout(fetchConnectedUsers, 500);
    })
    .catch(error => {
        addSystemMessage(`Erro ao registrar usuário: ${error.message}`);
    });
}

// Desregistrar usuário quando a página for fechada
window.addEventListener('beforeunload', function() {
    if (currentUsername) {
        navigator.sendBeacon('/api/tcp/users/unregister', 
            JSON.stringify({ username: currentUsername }));
    }
});

// Inicializar chat
function init() {
    // Mensagem de boas-vindas
    addSystemMessage(`Bem-vindo, ${currentUsername}`);
    
    // Conectar WebSocket e inicializar serviços
    connectWebSocket();
    fetchConnectedUsers();
    
    // Atualizar lista de usuários a cada 5 segundos
    setInterval(fetchConnectedUsers, 5000);
}

// Conectar ao WebSocket
function connectWebSocket() {
    const socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);
    stompClient.debug = null; // Desativar logs
    
    stompClient.connect({}, frame => {
        // Enviar mensagem de conexão
        stompClient.send("/app/connect", {}, JSON.stringify({
            sender: currentUsername,
            content: "connected",
            timestamp: new Date().toISOString(),
            type: 'CONNECTION'
        }));
        
        // Inscrever-se nos tópicos
        stompClient.subscribe('/topic/messages', message => {
            addMessage(JSON.parse(message.body));
        });
        
        stompClient.subscribe('/topic/notifications', message => {
            addSystemMessage(JSON.parse(message.body).content);
        });
        
        stompClient.subscribe('/topic/broadcast-messages', message => {
            addBroadcastMessage(JSON.parse(message.body));
        });
        
        // Inscrever-se no tópico de usuários
        stompClient.subscribe('/topic/users', message => {
            updateUserList(JSON.parse(message.body));
        });
        
        // Atualizar lista de usuários após conectar
        setTimeout(fetchConnectedUsers, 1000);
    }, error => {
        addSystemMessage('Conexão falhou. Tentando novamente em 5 segundos...');
        setTimeout(connectWebSocket, 5000);
    });
}

// Buscar usuários conectados
function fetchConnectedUsers() {
    fetch('/api/tcp/users')
        .then(response => {
            if (!response.ok) throw new Error(`Erro HTTP: ${response.status}`);
            return response.json();
        })
        .then(users => {
            updateUserList(users);
        })
        .catch(error => {
            document.getElementById('connectedUsers').innerHTML = 
                '<div class="text-center text-danger">Erro ao carregar usuários</div>';
        });
}

// Atualizar lista de usuários na interface
function updateUserList(users) {
    const usersContainer = document.getElementById('connectedUsers');
    usersContainer.innerHTML = '';
    
    // Verificar se há usuários
    if (!users || !Array.isArray(users) || users.length === 0) {
        usersContainer.innerHTML = '<div class="text-center text-muted">Nenhum usuário conectado</div>';
        return;
    }
    
    // Adicionar cada usuário à lista
    users.forEach(user => {
        if (!user) return;
        
        const userItem = document.createElement('div');
        userItem.className = 'user-item';
        
        const statusDot = document.createElement('span');
        statusDot.className = 'user-status-dot';
        
        const userName = document.createElement('span');
        userName.className = 'user-name';
        userName.textContent = user;
        
        userItem.appendChild(statusDot);
        userItem.appendChild(userName);
        
        if (user === currentUsername) {
            userItem.classList.add('user-self');
        }
        
        usersContainer.appendChild(userItem);
    });
}

// Adicionar mensagem ao chat
function addMessage(message) {
    const messageElement = document.createElement('div');
    messageElement.classList.add('message');
    
    // Definir classe baseada no remetente
    if (message.sender === currentUsername) {
        messageElement.classList.add('message-self');
    } else if (message.sender === 'System') {
        messageElement.classList.add('message-system');
    } else {
        messageElement.classList.add('message-other');
    }
    
    // Adicionar cabeçalho para mensagens de outros usuários
    if (message.sender !== 'System' && message.sender !== currentUsername) {
        const header = document.createElement('div');
        header.classList.add('message-header');
        header.textContent = message.sender;
        
        const time = document.createElement('span');
        time.classList.add('message-time');
        time.textContent = formatTime(message.timestamp);
        header.appendChild(time);
        
        messageElement.appendChild(header);
    }
    
    // Adicionar conteúdo da mensagem
    const content = document.createElement('div');
    content.textContent = message.content;
    messageElement.appendChild(content);
    
    // Adicionar hora para mensagens próprias
    if (message.sender === currentUsername) {
        const time = document.createElement('div');
        time.classList.add('message-time');
        time.textContent = formatTime(message.timestamp || new Date());
        messageElement.appendChild(time);
    }
    
    // Adicionar ao log e rolar para baixo
    messageLogElement.appendChild(messageElement);
    scrollToBottom();
}

// Adicionar mensagem do sistema
function addSystemMessage(text) {
    addMessage({
        sender: 'System',
        content: text,
        timestamp: new Date()
    });
}

// Adicionar mensagem de broadcast
function addBroadcastMessage(message) {
    const messageElement = document.createElement('div');
    messageElement.classList.add('message');
    
    // Aplicar classes específicas
    if (message.sender === currentUsername) {
        messageElement.classList.add('message-self', 'message-broadcast');
    } else {
        messageElement.classList.add('message-other', 'message-broadcast');
    }
    
    // Adicionar cabeçalho se não for mensagem própria
    if (message.sender !== currentUsername) {
        const header = document.createElement('div');
        header.classList.add('message-header');
        header.textContent = message.sender;
        
        const time = document.createElement('span');
        time.classList.add('message-time');
        time.textContent = formatTime(message.timestamp);
        header.appendChild(time);
        
        messageElement.appendChild(header);
    }
    
    // Adicionar conteúdo e hora
    const content = document.createElement('div');
    content.textContent = message.content;
    messageElement.appendChild(content);
    
    if (message.sender === currentUsername) {
        const time = document.createElement('div');
        time.classList.add('message-time');
        time.textContent = formatTime(message.timestamp || new Date());
        messageElement.appendChild(time);
    }
    
    messageLogElement.appendChild(messageElement);
    scrollToBottom();
}

// Formatar timestamp
function formatTime(timestamp) {
    if (!timestamp) return '';
    
    const date = typeof timestamp === 'string' ? new Date(timestamp) : timestamp;
    return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
}

// Rolar chat para baixo
function scrollToBottom() {
    messageLogElement.scrollTop = messageLogElement.scrollHeight;
}

// Enviar mensagem de chat
function sendChatMessage() {
    const messageInput = document.getElementById('chatMessage');
    const message = messageInput.value.trim();
    
    if (!message) return;

    // Enviar mensagem de broadcast
    if (stompClient) {
        stompClient.send("/app/chat", {}, JSON.stringify({
            sender: currentUsername,
            content: message,
            timestamp: new Date().toISOString(),
            type: 'BROADCAST'
        }));
    }
    
    // Limpar input e focar
    messageInput.value = '';
    messageInput.focus();
}

// Aviso ao sair da página
window.addEventListener('beforeunload', function(e) {
    const mensagem = 'Você será desconectado se sair da página.';
    e.returnValue = mensagem;
    return mensagem;
});
