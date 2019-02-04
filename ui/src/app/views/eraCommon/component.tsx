import {Card} from 'app/components/card';
import {SmallHeader} from 'app/components/headers';
import * as React from 'react';
import {Button} from '../../components/buttons';

// Change as per Greg PR nih-callback
const redirectUrl = 'http://localhost:4200/eraCommon?token={token}';

interface Props {
  eraCommon: boolean;
}

export class EraCommon extends React.Component<Props, any> {

  constructor(props: Props) {
    super(props);
  }

  loginToEraCommon() {
    window.location.href = 'http://mock-nih.dev.test.firecloud.org/link-nih-account?redirect-url=' +
       redirectUrl;
  }

  componentDidMount() {
    console.log(window.location.href);
  }

  render() {
    return <div>
      <SmallHeader>
        Researcher Workbench
      </SmallHeader>
      <div style={{display: 'flex', flexDirection: 'row', overflow: 'hidden'}}>
        <div>
          <div>
        In order to get access to data and tools please complete
        the following:
          </div>
          <div>
            Please login to your ERA Commons account and complete the online training coarses in order
            to gain full access to the Researcher Workbench data and tools.
            Diam nonummy nibh, euismod tincidunt ut laoreet dolore magna aliquam erat volutpat ut wisi.

          </div>
        </div>
        <div style={{display: 'flex', flexDirection: 'column', justifyContent: 'space-between', width: '10rem'}}>
          <Card style={{display: 'flex', flexDirection: 'row', justifyContent: 'space-between'}}>
            <div style={{display: 'flex', flexDirection: 'column'}}>
            <SmallHeader>
              Login to ERA Commons
            </SmallHeader>
            <div>
              Vel illum dolore eu feugiat nulla facilisis at vero
              eros et accumsan et iusto odio dignissim.
              Ullamcorper suscipit lortis nisl ex.
            </div>
            </div>
            <div style={{paddingTop: '1.3rem'}}>
              {!this.props.eraCommon &&
                <Button onClick={() => this.loginToEraCommon()}>LOGIN</Button>
              }
              {this.props.eraCommon &&
              <Card>Linked</Card>
              }
            </div>

          </Card>
          <Card style={{width: '26rem', height: '2rem'}}>
            <SmallHeader>
            Complete Online training
            </SmallHeader>
            <div>
              Some text
            </div>
          </Card>
        </div>
      </div>

    </div>;
  }
}

export default EraCommon;
