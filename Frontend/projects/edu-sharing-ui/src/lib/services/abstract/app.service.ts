export abstract class AppService {
    abstract isRunningApp(): boolean;
    abstract getLanguage(): Promise<string>;
}
