import { OptionData } from '../options-helper-data.service';
import { Target } from '../../types/option-item';
import { Node } from 'ngx-edu-sharing-api';

export abstract class OptionsHelperService {
    abstract wrapOptionCallbacks(data: OptionData): OptionData;

    abstract refreshComponents(refreshListOptions: boolean, data: OptionData);

    abstract getAvailableOptions(target: Target, objects: Node[], data: OptionData);
}
