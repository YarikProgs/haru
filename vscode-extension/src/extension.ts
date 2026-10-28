import * as path from 'path';
import * as vscode from 'vscode';
import {
    LanguageClient,
    LanguageClientOptions,
    ServerOptions,
} from 'vscode-languageclient/node';

let client: LanguageClient | undefined;

export function activate(context: vscode.ExtensionContext) {
    const javaPath =
        vscode.workspace.getConfiguration('haru').get<string>('javaPath') || 'java';
    const jarPath = context.asAbsolutePath(path.join('server', 'haru-language-server-shadow.jar'));

    const serverOptions: ServerOptions = {
        command: javaPath,
        args: ['-jar', jarPath],
    };

    const clientOptions: LanguageClientOptions = {
        documentSelector: [{ scheme: 'file', language: 'haru' }],
    };

    client = new LanguageClient(
        'haru',
        'Haru Language Server',
        serverOptions,
        clientOptions
    );
    client.start();
}

export function deactivate(): Thenable<void> | undefined {
    return client?.stop();
}