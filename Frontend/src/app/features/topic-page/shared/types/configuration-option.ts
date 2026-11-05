export class ConfigurationOption {
    icon: string;
    isVisible: boolean;
    text: string;
    tooltip?: string;

    constructor(isVisible: boolean = false, text: string = '') {
        this.isVisible = isVisible;
        this.text = text;
    }
}
