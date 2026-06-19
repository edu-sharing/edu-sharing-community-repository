import type { OptionsHelperService } from '../options-helper.service';
import { OptionData, OptionsHelperComponents } from 'ngx-edu-sharing-ui';
import { WorkspaceManagementDialogsComponent } from '../../features/management-dialogs/management-dialogs.component';

export interface OptionsContext {
    service: OptionsHelperService;
    management: WorkspaceManagementDialogsComponent;
    components: OptionsHelperComponents;
    data: OptionData;
    queryParams: Record<string, string>;
}
