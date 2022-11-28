import { Node } from '../../../../core-module/core.module';

export type LicenseDialogData =
    | {
          kind: 'nodes';
          nodes: Node[];
      }
    | {
          kind: 'properties';
          properties: any;
      };

export type LicenseDialogResult = Node[] | any | null;
