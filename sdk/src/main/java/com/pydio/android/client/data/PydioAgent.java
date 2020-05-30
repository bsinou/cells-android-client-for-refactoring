package com.pydio.android.client.data;

import com.pydio.android.client.data.auth.AppCredentials;
import com.pydio.android.client.data.db.Database;
import com.pydio.android.client.services.Cache;
import com.pydio.android.client.services.Connectivity;
import com.pydio.android.client.utils.Background;
import com.pydio.android.client.utils.Task;
import com.pydio.sdk.core.Client;
import com.pydio.sdk.core.Pydio;
import com.pydio.sdk.core.common.callback.Completion;
import com.pydio.sdk.core.common.callback.JSONCompletion;
import com.pydio.sdk.core.common.callback.MessageCompletion;
import com.pydio.sdk.core.common.callback.NodeListCompletion;
import com.pydio.sdk.core.common.callback.RegistryItemHandler;
import com.pydio.sdk.core.common.callback.StringCompletion;
import com.pydio.sdk.core.common.callback.TransferProgressListener;
import com.pydio.sdk.core.common.callback.WorkspaceCompletion;
import com.pydio.sdk.core.common.callback.WorkspaceListCompletion;
import com.pydio.sdk.core.common.errors.Code;
import com.pydio.sdk.core.common.errors.Error;
import com.pydio.sdk.core.common.errors.SDKException;
import com.pydio.sdk.core.model.FileNode;
import com.pydio.sdk.core.model.Message;
import com.pydio.sdk.core.model.Node;
import com.pydio.sdk.core.model.ServerNode;
import com.pydio.sdk.core.model.WorkspaceNode;
import com.pydio.sdk.core.security.Credentials;
import com.pydio.sdk.core.server.Plugin;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static com.pydio.sdk.core.Client.get;

public class PydioAgent {

    public Session session;
    public Client client;

    // Constructors
    public PydioAgent(ServerNode node){
        this.session = new Session();
        this.session.server = node;
        this.client = Client.get(node);
        this.client.setTokenProvider(Database::getToken);
        this.client.setTokenStore(Database::saveToken);
    }

    public PydioAgent(Session s) {
        this.session = s;
        this.client = Client.get(s.server);
        AppCredentials credentials = new AppCredentials(this.session.server.url());
        credentials.setLogin(session.user);
        this.client.setCredentials(credentials);
        this.client.setTokenProvider(Database::getToken);
        this.client.setTokenStore(Database::saveToken);
    }

    public static PydioAgent createFromID(String server) {
        int index = server.lastIndexOf('@');
        String user = server.substring(0, index);
        String url = server.substring(index + 1);

        url = url.replace("+", "://");
        url = url.replace("&", "/");

        ServerNode serverNode = null;
        try {
            serverNode = ServerNode.fromAddress(url);
            PydioAgent agent = new PydioAgent(serverNode);
            agent.session.user = user;
            agent.client = get(serverNode);
            agent.client.setTokenProvider(Database::getToken);
            agent.client.setTokenStore(Database::saveToken);

            AppCredentials credentials = new AppCredentials(agent.session.server.url());
            credentials.setLogin(agent.session.user);
            agent.client.setCredentials(credentials);
            return agent;

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void setCredentials(Credentials credentials) {
        this.client.setCredentials(credentials);
    }

    // Workspace Slug
    private WorkspaceNode findWorkspaceNodeById(String workspaceId) {
        return session.server.getWorkspace(workspaceId);
    }

    public String workspaceSlug(Node node) {
        if (node.type() == Node.TYPE_WORKSPACE) {
            return node.id();
        } else {
            String slug = node.getProperty(Pydio.NODE_PROPERTY_WORKSPACE_SLUG);
            if (slug == null || "".equals(slug)) {
                WorkspaceNode ws = session.server.findWorkspaceById(node.getProperty(Pydio.NODE_PROPERTY_WORKSPACE_UUID));
                if (ws != null) {
                    slug = ws.slug();
                }
            }
            return slug;
        }
    }

    public boolean supportOAuth() {
        return this.session.server.supportsOauth();
    }

    // API calls
    public Task logout() {
        return Background.go(() -> {
            try {client.logout();}catch (Exception ignore) {} catch (SDKException ignore) {}
        });
    }

    public Task workspaces(WorkspaceListCompletion h) {
        return Background.go(() -> {
            List<WorkspaceNode> workspaceNodes = new ArrayList<>();
            try  {
                client.workspaceList((node) -> workspaceNodes.add((WorkspaceNode) node));
                session.server.setWorkspaces(workspaceNodes);
                h.onComplete(workspaceNodes, null);
            } catch (SDKException e) {
                h.onComplete(null, Error.fromException(e));
            }
        });
    }

    public Task workspaceInfo(String id, WorkspaceCompletion wc) {
        return Background.go(() -> {

            final WorkspaceNode[] newNode = {null};
            final List<String> actions = new ArrayList<>();
            final Properties preferences = new Properties();
            final List<Plugin> plugins = new ArrayList<>();

            RegistryItemHandler handler = new RegistryItemHandler() {
                @Override
                public void onPref(String name, String value) {
                    if(name == null || value== null){
                        return;
                    }
                    preferences.setProperty(name, value);
                }

                @Override
                public void onAction(String action, String read, String write) {
                    super.onAction(action, read, write);
                    actions.add(action + ":" + read + "|" + write);
                }

                @Override
                public void onWorkspace(Properties p) {
                    newNode[0] = new WorkspaceNode();
                    newNode[0].setProperties(p);
                }

                @Override
                public void onPlugin(Plugin p) {
                    plugins.add(p);
                }
            };

            WorkspaceNode node = session.server.getWorkspace(id);
            try  {
                client.downloadWorkspaceRegistry(id, handler);
                if (node == null) {
                    wc.onComplete(null, new Error(Code.not_found));
                } else {
                    node.setActions(actions);
                    node.setPreferences(preferences);
                    node.setPlugins(plugins);
                    wc.onComplete(node, null);
                }
            } catch (SDKException e) {
                e.printStackTrace();
                node.setActions(actions);
                node.setPreferences(preferences);
                node.setPlugins(plugins);
                wc.onComplete(null, Error.fromException(e));
            }
        });
    }

    public Task ls(Node node, boolean cache, int count, int offset, NodeListCompletion c) {
        String ws = workspaceSlug(node);
        if(cache) {
            return cacheList(ws, node.path(), count, offset, c);
        }
        return remoteList(ws, node.path(), count, offset, c);
    }

    private Task cacheList(final String ws, final String folder, int count, int offset, NodeListCompletion c) {
        return Background.go(()-> {
            List<Node> nodes = new ArrayList<>();
            final String cacheWs = session.workspaceCacheID(ws);
            Cache.nodes(cacheWs, folder, nodes::add);
            if (nodes.size() == 0 && !Connectivity.isConnectedToTheInternet()) {
                String offlinePath = session.workspacePath(ws);
                File folderFile = new File(offlinePath + folder);
                if (folderFile.isDirectory()) {
                    for (File child: folderFile.listFiles()) {
                        FileNode fileNode = LocalFS.fileToNode(child);
                        fileNode.setProperty(Pydio.NODE_PROPERTY_PATH, child.getPath().replaceFirst(offlinePath, ""));
                        fileNode.setProperty(Pydio.NODE_PROPERTY_FILENAME, child.getPath().replaceFirst(offlinePath, ""));
                        nodes.add(fileNode);
                    }
                }
            }
            c.onComplete(nodes, null);
        });
    }

    private Task remoteList(final String ws, final String folder, int count, int offset, NodeListCompletion c) {
        return Background.go(() -> {
            List<Node> nodes = new ArrayList<>();
            if(Connectivity.isConnectedToTheInternet()) {
                try {
                    client.ls(ws, folder, nodes::add);
                    c.onComplete(nodes, null);

                } catch (SDKException e) {
                    if(e.code == 401) {
                        if (!this.session.server.supportsOauth()) {
                            Database.deleteToken(session.tokenKey());
                        }
                        try {
                            client.ls(ws, folder, nodes::add);
                            c.onComplete(nodes, null);
                        } catch (SDKException e1) {
                            c.onComplete(null, Error.fromException(e));
                        }
                        return;
                    }
                    c.onComplete(null, Error.fromException(e));
                }
            } else {
                String offlinePath = session.workspacePath(ws);
                File folderFile = new File(offlinePath + folder);
                if (folderFile.exists()) {
                    for (File child: folderFile.listFiles()) {
                        if (child.getName().startsWith(".")){
                            continue;
                        }
                        FileNode fileNode = LocalFS.fileToNode(child);
                        fileNode.setProperty(Pydio.NODE_PROPERTY_PATH, child.getPath().replaceFirst(offlinePath, ""));
                        fileNode.setProperty(Pydio.NODE_PROPERTY_FILENAME, child.getPath().replaceFirst(offlinePath, ""));
                        nodes.add(fileNode);
                    }
                    c.onComplete(nodes, null);
                } else {
                    c.onComplete(null, new Error(Code.no_internet));
                }
            }
        });
    }

    public Task getBookmarks(NodeListCompletion c) {
        return Background.go(()-> {
            List<Node> nodes = new ArrayList<>();
            Error error = null;
            try {
                this.client.bookmarks(nodes::add);
            } catch (SDKException e) {
                e.printStackTrace();
                error = Error.fromException(e);
            }
            c.onComplete(nodes, error);
        });
    }

    public Task search(Node node, String text, NodeListCompletion c) {
        return Background.go(() -> {
            try {
                List<Node> list = new ArrayList<>();
                String slug = workspaceSlug(node);
                client.search(slug, node.path(), text, list::add);
                for (Node n: list) {
                    n.setProperty(Pydio.NODE_PROPERTY_WORKSPACE_SLUG, workspaceSlug(node));
                }
                c.onComplete(list, null);
            } catch (SDKException e) {
                c.onComplete(null, Error.fromException(e));
            }
        });
    }

    public Task download(Node node, File target, TransferProgressListener progressListener, Completion completion) {
        return Background.go(() -> {
            try {
                client.download(workspaceSlug(node), node.path(), target, progressListener);
                if(completion != null){
                    completion.onComplete(null);
                }
            } catch (SDKException e) {
                if(completion != null){
                    completion.onComplete(Error.fromException(e));
                }
            }
        });
    }

    public Task upload(Node node, File source, final TransferProgressListener progressListener, MessageCompletion completion){
        return Background.go(() -> {
            String name = source.getName();
            try {
                Message msg = client.upload(source, workspaceSlug(node), node.path(), name, true, progressListener);
                if(completion != null) {
                    if (msg != null) {
                        for (Node n: msg.added) {
                            n.setProperty(Pydio.NODE_PROPERTY_WORKSPACE_SLUG, workspaceSlug(node));
                        }
                        for (Node n: msg.updated) {
                            n.setProperty(Pydio.NODE_PROPERTY_WORKSPACE_SLUG, workspaceSlug(node));
                        }
                    }
                    completion.onComplete(msg, null);
                }
            } catch (SDKException e) {
                if(completion != null) {
                    completion.onComplete(null, Error.fromException(e));
                }
            }
        });
    }

    public Task upload(Node node, File[] sources, final TransferProgressListener progressListener, MessageCompletion completion){
        return Background.go(() -> {
            Message resultMessage = null;
            for (File source: sources){
                String name = source.getName();
                try {
                    Message msg = client.upload(source, workspaceSlug(node), node.path(), name, true, progressListener);
                    if(msg != null) {
                        if( resultMessage == null) {
                            resultMessage = new Message();
                        }
                        resultMessage.added.addAll(msg.added);
                        resultMessage.updated.addAll(msg.updated);
                        resultMessage.deleted.addAll(msg.deleted);
                    }
                } catch (SDKException ignore) {}
            }

            if(completion != null && resultMessage != null) {
                for (Node n: resultMessage.added) {
                    n.setProperty(Pydio.NODE_PROPERTY_WORKSPACE_SLUG, workspaceSlug(node));
                }
                for (Node n: resultMessage.updated) {
                    n.setProperty(Pydio.NODE_PROPERTY_WORKSPACE_SLUG, workspaceSlug(node));
                }
                completion.onComplete(resultMessage, null);
            }
        });
    }

    public Task copy(List<Node> nodes, Node to, MessageCompletion completion) {
        return Background.go(() -> {
            String ws = null;
            String[] paths = new String[nodes.size()];
            for (int i = 0; i < nodes.size(); i++) {
                Node node = nodes.get(i);
                if (ws == null) {
                    ws = workspaceSlug(node);
                }
                paths[i] = node.path();
            }

            try {
                Message msg = client.copy(ws, paths, to.path());
                if (msg != null) {
                    for (Node n: msg.added) {
                        n.setProperty(Pydio.NODE_PROPERTY_WORKSPACE_SLUG, ws);
                    }
                    for (Node n: msg.updated) {
                        n.setProperty(Pydio.NODE_PROPERTY_WORKSPACE_SLUG, ws);
                    }
                }
                completion.onComplete(msg, null);
            } catch (SDKException e) {
                if(completion != null) {
                    completion.onComplete(null, Error.fromException(e));
                }
            }
        });
    }

    public Task move(List<Node> nodes, Node to, MessageCompletion completion) {
        return Background.go(() -> {
            String ws = null;
            String[] paths = new String[nodes.size()];
            for (int i = 0; i < nodes.size(); i++) {
                Node node = nodes.get(i);
                if (ws == null) {
                    ws = workspaceSlug(node);
                }
                paths[i] = node.path();
            }
            try {
                Message msg = client.move(ws, paths, to.path());
                if (msg != null) {
                    for (Node n: msg.added) {
                        n.setProperty(Pydio.NODE_PROPERTY_WORKSPACE_SLUG, ws);
                    }
                    for (Node n: msg.updated) {
                        n.setProperty(Pydio.NODE_PROPERTY_WORKSPACE_SLUG, ws);
                    }
                }
                completion.onComplete(msg, null);
            } catch (SDKException e) {
                if(completion != null) {
                    completion.onComplete(null, Error.fromException(e));
                }
            }
        });
    }

    public Task delete(Node[] nodes, MessageCompletion completion) {
        return Background.go(() -> {
            for (Node node: nodes) {
                try {
                    Message msg  = client.delete(workspaceSlug(node), new String[]{node.path()});
                    if(completion != null) {
                        if (msg != null) {
                            for (Node n: msg.added) {
                                n.setProperty(Pydio.NODE_PROPERTY_WORKSPACE_SLUG, workspaceSlug(node));
                            }
                            for (Node n: msg.updated) {
                                n.setProperty(Pydio.NODE_PROPERTY_WORKSPACE_SLUG, workspaceSlug(node));
                            }
                        }
                        completion.onComplete(msg, null);
                    }
                } catch (SDKException e) {
                    if(completion != null) {
                        completion.onComplete(null, Error.fromException(e));
                    }
                }
            }
        });
    }

    public Task rename (Node node, String newName, MessageCompletion completion)  {
        return Background.go(() -> {
            try {
                Message msg = client.rename(workspaceSlug(node), node.path(), newName);
                if(completion != null) {
                    if (msg != null) {
                        for (Node n: msg.added) {
                            n.setProperty(Pydio.NODE_PROPERTY_WORKSPACE_SLUG, workspaceSlug(node));
                        }
                        for (Node n: msg.updated) {
                            n.setProperty(Pydio.NODE_PROPERTY_WORKSPACE_SLUG, workspaceSlug(node));
                        }
                    }
                    completion.onComplete(msg, null);
                }
            } catch (SDKException e) {
                if(completion != null) {
                    completion.onComplete(null, Error.fromException(e));
                }
            }
        });
    }

    public Task createDir(Node dirNode, String name, MessageCompletion completion) {
        return Background.go(() -> {
            try {
                Message msg = client.mkdir(workspaceSlug(dirNode), dirNode.path(), name);
                if(completion != null) {
                    if (msg != null) {
                        for (Node node: msg.added) {
                            node.setProperty(Pydio.NODE_PROPERTY_WORKSPACE_SLUG, workspaceSlug(dirNode));
                        }
                        for (Node node: msg.updated) {
                            node.setProperty(Pydio.NODE_PROPERTY_WORKSPACE_SLUG, workspaceSlug(dirNode));
                        }
                    }
                    completion.onComplete(msg, null);
                }
            } catch (SDKException e) {
                if(completion != null) {
                    completion.onComplete(null, Error.fromException(e));
                }
            }
        });
    }

    public Task generateShareLink(Node node, String password, boolean folder, StringCompletion completion) {
        return Background.go(() -> {
            String link = null;
            Error error = null;
            try {
                String name =  new File(node.path()).getName();
                link  = client.share(workspaceSlug(node), node.id(), name, folder, name, password.trim(), 30, 30, true, true);
            } catch (SDKException e) {
                error = Error.fromException(e);
            }

            if (completion != null) {
                completion.onComplete(link, error);
            }
        });
    }

    public Task shareInfo(Node node, JSONCompletion completion){
        return Background.go(()-> {
            JSONObject info = null;
            Error error = null;

            try {
                info = client.shareInfo(workspaceSlug(node), node.getProperty(Pydio.NODE_PROPERTY_SHARE_UUID));
            } catch (SDKException e) {
                error = Error.fromException(e);
            }

            if(completion != null){
                completion.onComplete(info, error);
            }
        });
    }

    public Task deleteShareLink(Node node, Completion completion){
        return Background.go(()-> {
            Error error = null;
            try {
                if (this.session.server.versionName().contains("cells")) {
                    client.unshare(workspaceSlug(node), node.getProperty(Pydio.NODE_PROPERTY_SHARE_UUID));
                } else {
                    client.unshare(workspaceSlug(node), node.path());
                }
            } catch (SDKException e) {
                error = Error.fromException(e);
            }
            if(completion != null){
                completion.onComplete(error);
            }
        });
    }

    public Task downloadURL(Node node, StringCompletion completion){
        return Background.go(() -> {
            String url = null;
            Error error = null;
            try {
                url = client.downloadURL(workspaceSlug(node), node.path());
            } catch (SDKException e) {
                error = Error.fromException(e);
            }
            completion.onComplete(url, error);
        });
    }

    public Task restore(Node node, MessageCompletion completion) {
        return Background.go(() -> {
            Error error = null;
            Message msg = null;
            try {
                msg = client.restore(workspaceSlug(node), new String[]{node.path()});
            } catch (SDKException e) {
                e.printStackTrace();
                error = Error.fromException(e);
            }
            completion.onComplete(msg, error);
        });
    }

    public Task emptyRecycleBin(String ws, MessageCompletion completion) {
        return Background.go(()->{
            Error error = null;
            Message msg = null;
            try {
                msg = client.emptyRecycleBin(ws);
            } catch (SDKException e) {
                e.printStackTrace();
                error = Error.fromException(e);
            }
            completion.onComplete(msg, error);
        });
    }

    public Task bookmark(Node node, MessageCompletion completion) {
        return Background.go(()->{
            try {
                client.bookmark(workspaceSlug(node), node.id());
                completion.onComplete(null, null);
            } catch (SDKException e) {
                e.printStackTrace();
                completion.onComplete(null, Error.fromException(e));
            }
        });
    }

    public Task unbookmark(Node node, MessageCompletion completion) {
        return Background.go(()->{
            try {
                client.unbookmark(workspaceSlug(node), node.id());
                completion.onComplete(null, null);
            } catch (SDKException e) {
                e.printStackTrace();
                completion.onComplete(null, Error.fromException(e));
            }
        });
    }
}
